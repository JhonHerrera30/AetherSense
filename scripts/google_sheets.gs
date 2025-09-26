/**
 * PROJECT 101 — Formato allineato a FIRE
 * - spec: [{ key, label, min, max }] con label normalizzata "Component-Measure-Unit"
 * - payload: meta + stesse chiavi della spec con i valori
 * - indicator separati; i valori devono stare anche nel payload
 */

/** ========= UTILS ========= **/
function normalize(value, divisor) {
  if (value === undefined || value === null) return 0;
  value = value.toString().trim().replace(",", ".");
  var n = parseFloat(value);
  if (isNaN(n)) return 0;
  return n / (divisor || 1);
}
function flag01(v) {
  if (v === undefined || v === null) return 0;
  var s = v.toString().trim().toLowerCase();
  return (s === "1" || s === "true") ? 1 : 0;
}
function prettyNow(fmt) {
  return Utilities.formatDate(new Date(), "Europe/Rome", fmt);
}

/** ========= HEADER & FIELD MAPS ========= **/
var HEADERS = [
  "Date",
  "Time [Europe/Rome]",
  "MAC Address",
  "SCD30 CO2 [ppm]",
  "SEN55 PM1.0 [µg/m³]",
  "SEN55 PM2.5 [µg/m³]",
  "SEN55 PM4.0 [µg/m³]",
  "SEN55 PM10.0 [µg/m³]",
  "SEN55 VOC [index]",
  "SEN55 NOx [index]",
  "SEN55 Fan Error",
  "SEN55 Speed Warning",
  "SEN55 Laser Error",
  "SEN55 RHT Error",
  "SEN55 Gas Error",
  "SEN55 Cleaning Active",
  "BME680 Temperature [°C]",
  "BME680 Humidity [%RH]",
  "BME680 Pressure [Pa]",
  "BME680 Gas [Ω]",
  "D7S EQ Flag",
  "D7S SI [m/s]",
  "D7S PGA [m/s^2]",
  "Battery Voltage [V]",
  "Battery Percentage [%]",
  "Latitude",
  "Longitude"
];

// Campi numerici da includere in spec/payload
var SPEC_INDEXES = (function () {
  var idx = [];
  for (var i = 3; i <= 9; i++) idx.push(i);      // SCD30 + SEN55 valori
  for (var j = 16; j <= 19; j++) idx.push(j);    // BME680
  idx.push(21, 22);                              // D7S SI / PGA
  return idx;
})();
// Indicator booleani separati
var INDICATOR_INDEXES = [10, 11, 12, 13, 14, 15, 20];

/** Range di misura — aggiornati per D7S */
var RANGE_MAP = {
  "SCD30 CO2 [ppm]": [0, 40000],
  "SEN55 PM1.0 [µg/m³]": [0, 1000],
  "SEN55 PM2.5 [µg/m³]": [0, 1000],
  "SEN55 PM4.0 [µg/m³]": [0, 1000],
  "SEN55 PM10.0 [µg/m³]": [0, 1000],
  "SEN55 VOC [index]": [0, 500],
  "SEN55 NOx [index]": [0, 500],
  "BME680 Temperature [°C]": [-40, 85],
  "BME680 Humidity [%RH]": [0, 100],
  "BME680 Pressure [Pa]": [30000, 110000],
  "BME680 Gas [Ω]": [0, 500000],
  // D7S (datasheet):
  // Acceleration Detection Range: ±2000 gal => 0..20 m/s^2 per il picco
  "D7S SI [m/s]": [0, 2],          // inferito dal grafico SI (1..100 kine = 0.01..1.0 m/s)
  "D7S PGA [m/s^2]": [0, 20]       // da ±2000 gal -> 20 m/s^2
};

/** ========= NORMALIZZAZIONE LABEL ========= **/
function splitHeader(h) {
  var m = h.match(/^(\S+)\s+([^\[]+?)(?:\s*\[([^\]]+)\])?$/);
  if (!m) return { component: h, measure: "", unit: "" };
  return {
    component: (m[1] || "").trim(),
    measure: (m[2] || "").trim(),
    unit: (m[3] || "").trim()
  };
}
function slugKeepDots(s) {
  if (!s) return "";
  return String(s).replace(/\s+/g, "").normalize("NFKD").replace(/[^\w.\-]/g, "");
}
function normalizeUnitKeepSymbols(u) {
  if (!u) return "none";
  var raw = String(u).trim();

  if (/^(°\s*C|degC|°C)$/i.test(raw)) return "°C";
  if (/^%?\s*RH$/i.test(raw) || /^%RH$/i.test(raw) || raw === "%") return "%RH";
  if (/^index$/i.test(raw) || /index/i.test(raw)) return "index";
  if (/µ\s*g\s*\/\s*m\s*³/i.test(raw) || /ug\/m3/i.test(raw) || /µg\/m³/i.test(raw)) return "ug/m3";
  if (raw === "Ω" || /ohm/i.test(raw)) return "Ω";
  if (/^Pa$/i.test(raw)) return "Pa";

  var s = raw.replace(/\s+/g, "")
             .replace(/µ/g, "u")
             .replace(/³/g, "3")
             .replace(/m³/g, "m3");
  s = s.normalize("NFKD").replace(/[^\w.%°Ω\/\-]/g, "");
  return s || "none";
}
function normalizedLabelFromHeader(h) {
  var parts = splitHeader(h);
  var comp = slugKeepDots(parts.component);
  var meas = slugKeepDots(parts.measure);
  var unit = normalizeUnitKeepSymbols(parts.unit);
  return [comp, meas, unit].join("-");
}
function indicatorLabelFromHeader(h) {
  var m = h.match(/^(\S+)\s+(.+)$/);
  var comp = slugKeepDots(m ? m[1] : h);
  var meas = slugKeepDots(m ? m[2] : "").replace(/-/g, "");
  return [comp, meas, "none"].join("-");
}

/** ========= SHEET HELPERS ========= **/
function setHeaders(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(HEADERS);
    return;
  }
  ensureColumns(sheet, HEADERS.length);
  var range = sheet.getRange(1, 1, 1, HEADERS.length);
  var current = range.getValues()[0];
  var need = false;
  for (var i = 0; i < HEADERS.length; i++) {
    if ((current[i] || "").toString().trim() === "") {
      need = true;
      break;
    }
  }
  if (need) range.setValues([HEADERS]);
}
function ensureColumns(sheet, minColumns) {
  var current = sheet.getMaxColumns();
  if (current < minColumns) sheet.insertColumnsAfter(current, minColumns - current);
}

/** ========= ENTRYPOINT ========= **/
function doGet(e) {
  try {
    if (!e.parameter || Object.keys(e.parameter).length === 0) {
      return ContentService.createTextOutput("No parameters received");
    }
    var p = e.parameter;
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("Foglio1") || ss.getSheets()[0];

    setHeaders(sheet);
    ensureColumns(sheet, HEADERS.length);

    var row = new Array(HEADERS.length);

    // ---- meta ----
    row[0] = p.currentDate || prettyNow("yyyy-MM-dd");
    row[1] = p.currentTime || prettyNow("HH:mm:ss");
    row[2] = p.macAddress || "";

    // ---- SCD30 ----
    row[3] = normalize(p.co2_ppm);

    // ---- SEN55 valori ----
    row[4] = normalize(p.pm1p0);
    row[5] = normalize(p.pm2p5);
    row[6] = normalize(p.pm4p0);
    row[7] = normalize(p.pm10p0);
    row[8] = normalize(p.vocIndex);
    row[9] = normalize(p.noxIndex);

    // ---- SEN55 flags (indicator) ----
    row[10] = flag01(p.sen55_fan_err);
    row[11] = flag01(p.sen55_speed_warn);
    row[12] = flag01(p.sen55_laser_err);
    row[13] = flag01(p.sen55_rht_err);
    row[14] = flag01(p.sen55_gas_err);
    row[15] = flag01(p.sen55_cleaning);

    // ---- BME680 (x100 -> unità reali) ----
    row[16] = normalize(p.bmeT_x100, 100);
    row[17] = normalize(p.bmeRH_x100, 100);
    row[18] = normalize(p.bmeP_x100, 100);
    row[19] = normalize(p.bmeGas_x100, 100);

    // ---- D7S ----
    row[20] = flag01(p.d7s_eq_flag);
    row[21] = normalize(p.d7s_SI_ms_x100, 100);
    row[22] = normalize(p.d7s_PGA_ms2_x100, 100);

    // ---- Battery ----
    row[23] = normalize(p.bat_v_x100, 100);
    row[24] = normalize(p.bat_pct);

    // ---- GNSS ----
    row[25] = normalize(p.latitude);
    row[26] = normalize(p.longitude);

    // salva riga su sheet
    sheet.getRange(sheet.getLastRow() + 1, 1, 1, row.length).setValues([row]);

    // ---- indicatori ----
    var indicator = [];
    var indicatorPayload = {};
    var indicatorKeySet = {};
    for (var h = 0; h < INDICATOR_INDEXES.length; h++) {
      var iidx = INDICATOR_INDEXES[h];
      var hname = HEADERS[iidx];
      var ilabel = indicatorLabelFromHeader(hname);
      var flag = Number(!!row[iidx]);
      indicator.push(ilabel);
      indicatorPayload[ilabel] = flag;
      indicatorKeySet[ilabel] = true;
    }

    // ---- payload/spec (stile FIRE) ----
    var payload = {
      currentDate: row[0],
      currentTime: row[1],
      macAddress: row[2],
      bat_V: row[23],
      bat_pct: row[24],
      latitude: row[25],
      longitude: row[26]
    };

    var spec = [];
    for (var k = 0; k < SPEC_INDEXES.length; k++) {
      var idx = SPEC_INDEXES[k];
      var header = HEADERS[idx];
      var label = normalizedLabelFromHeader(header);
      if (indicatorKeySet[label]) {
        continue; // evita schede duplicate per i flag
      }
      var val = row[idx];
      var isNum = (val !== "" && val !== null && typeof val === "number" && !isNaN(val));
      if (!isNum) continue;

      var range = RANGE_MAP[header] || [null, null];
      spec.push({ key: label, label: label, min: range[0], max: range[1] });
      payload[label] = val;
    }

    for (var key in indicatorPayload) {
      if (indicatorPayload.hasOwnProperty(key)) {
        payload[key] = indicatorPayload[key]; // necessario: il backend legge i flag dal payload
      }
    }

    var typeOfDevice = (p.typeOfDevice ? String(p.typeOfDevice).trim() : "") || "Device4G";

    try {
      var resp = UrlFetchApp.fetch("https://sensors.joinyourteam.it/api/packets", {
        method: "post",
        contentType: "application/json",
        muteHttpExceptions: true,
        payload: JSON.stringify({
          projectId: 101,
          typeOfDevice: typeOfDevice,
          macAddress: payload.macAddress,
          payload: payload,
          latitude: payload.latitude,
          longitude: payload.longitude,
          spec: spec,
          indicator: indicator,
          currentDate: payload.currentDate,
          currentTime: payload.currentTime
        })
      });
      var code = resp.getResponseCode();
      if (code === 200) {
        Logger.log("Packet accepted: " + resp.getContentText());
      } else {
        Logger.log("Packet rejected: " + code + " " + resp.getContentText());
      }
    } catch (errApi) {
      Logger.log("Error calling sensors.joinyourteam API: " + errApi);
    }

    return ContentService.createTextOutput("Ok");
  } catch (err) {
    return ContentService.createTextOutput("Error in script: " + err);
  }
}
