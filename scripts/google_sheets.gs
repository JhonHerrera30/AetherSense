/**
 * Normalizza un valore numerico passato come stringa.
 * - converte virgola in punto
 * - se vuoto o non numerico -> 0
 * - divide per "divisor" (default 1)
 */
function normalize(value, divisor = 1) {
  if (value === undefined || value === null) return 0;
  value = value.toString().trim().replace(",", ".");
  var n = parseFloat(value);
  if (isNaN(n)) return 0;
  return n / divisor;
}

/** Intestazioni colonne (0-based):
 *  0 Data, 1 Ora, 2 MAC,
 *  3 CO2, 4 PM1.0, 5 PM2.5, 6 PM4.0, 7 PM10,
 *  8 SEN T, 9 SEN RH, 10 VOC, 11 NOx,
 *  12 BME T, 13 BME RH, 14 BME P, 15 BME Gas,
 *  16 Alt m, 17 BAT_V, 18 BAT_%,
 *  19 Lat, 20 Lon,
 *  21 ICM_T, 22 ICM_AccX, 23 ICM_AccY, 24 ICM_AccZ,
 *  25 ICM_GyrX, 26 ICM_GyrY, 27 ICM_GyrZ
 */
var HEADERS = [
  "Data",
  "Ora [Europe/Rome]",
  "MAC Address",
  "SCD30 CO2 [ppm]",
  "SEN55 PM1.0 [µg/m³]",
  "SEN55 PM2.5 [µg/m³]",
  "SEN55 PM4.0 [µg/m³]",
  "SEN55 PM10.0 [µg/m³]",
  "SEN55 Temp [°C]",
  "SEN55 RH [%]",
  "SEN55 VOC [index]",
  "SEN55 NOx [index]",
  "BME680 Temp [°C]",
  "BME680 RH [%]",
  "BME680 Pressione [Pa]",
  "BME680 Gas [Ω]",
  "BME680 Altitudine [m]",
  "BAT_V [V]",
  "BAT_% [%]",
  "Latitudine",
  "Longitudine",
  "ICM Temp [°C]",
  "ICM AccX [g]",
  "ICM AccY [g]",
  "ICM AccZ [g]",
  "ICM GyrX [°/s]",
  "ICM GyrY [°/s]",
  "ICM GyrZ [°/s]"
];

/**
 * Entry point: riceve parametri GET dal firmware, scrive su Google Sheets
 * e inoltra i dati ad AetherSense.
 */
function doGet(e) {
  try {
    if (!e.parameter || Object.keys(e.parameter).length === 0) {
      return ContentService.createTextOutput("Nessun parametro ricevuto");
    }

    var payload = e.parameter;

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("Foglio1") || ss.getSheets()[0];

    setHeaders(sheet);
    ensureColumns(sheet, HEADERS.length);

    var row = new Array(HEADERS.length);
    var now = new Date();

    // Data / Ora (fallback locale Europe/Rome)
    row[0] = e.parameter.currentDate || Utilities.formatDate(now, "Europe/Rome", "yyyy-MM-dd");
    row[1] = e.parameter.currentTime || Utilities.formatDate(now, "Europe/Rome", "HH:mm:ss");

    // MAC come terza colonna
    row[2] = e.parameter.macAddress || "";

    // SCD30
    row[3] = normalize(e.parameter.co2_ppm, 1);        // ppm

    // SEN55
    row[4] = normalize(e.parameter.pm1p0, 1);          // µg/m³
    row[5] = normalize(e.parameter.pm2p5, 1);
    row[6] = normalize(e.parameter.pm4p0, 1);
    row[7] = normalize(e.parameter.pm10p0, 1);
    row[8] = normalize(e.parameter.senT, 1);           // °C
    row[9] = normalize(e.parameter.senRH, 1);          // %
    row[10] = normalize(e.parameter.vocIndex, 1);
    row[11] = normalize(e.parameter.noxIndex, 1);

    // BME680 (arrivano x100 dal firmware)
    row[12] = normalize(e.parameter.bmeT_x100,   100); // °C
    row[13] = normalize(e.parameter.bmeRH_x100,  100); // %
    row[14] = normalize(e.parameter.bmeP_x100,   100); // Pa
    row[15] = normalize(e.parameter.bmeGas_x100, 100); // Ω

    // Altitudine (x100 -> m)
    row[16] = normalize(e.parameter.alt_x100, 100);

    // Batteria: V in x100, % già 0..100
    row[17] = normalize(e.parameter.bat_v_x100, 100);  // V
    row[18] = normalize(e.parameter.bat_pct, 1);       // %

    // Lat/Lon (se arrivano in microgradi)
    row[19] = normalize(e.parameter.latitude,  1);
    row[20] = normalize(e.parameter.longitude, 1);

    // ICM-20948 (tutti x100 dal firmware)
    row[21] = normalize(e.parameter.icmTemp_x100,     100); // °C
    row[22] = normalize(e.parameter.icmAccX_gx100,    100); // g
    row[23] = normalize(e.parameter.icmAccY_gx100,    100); // g
    row[24] = normalize(e.parameter.icmAccZ_gx100,    100); // g
    row[25] = normalize(e.parameter.icmGyrX_dpsx100,  100); // °/s
    row[26] = normalize(e.parameter.icmGyrY_dpsx100,  100); // °/s
    row[27] = normalize(e.parameter.icmGyrZ_dpsx100,  100); // °/s

    // Scrivi una riga
    sheet.getRange(sheet.getLastRow() + 1, 1, 1, row.length).setValues([row]);

    // Invio pacchetto a AetherSense
    try {
      // Costruisce la specifica delle misurazioni
      var spec = HEADERS
        .filter(function(h, i) {
          return [0, 1, 2, 17, 18, 19, 20].indexOf(i) === -1;
        })
        .map(function(h) {
          var match = h.match(/^([^ ]+)\s+([^\[]+)\s+\[([^\]]+)\]$/);
          if (match) {
            return match[1] + '-' +
                   match[2].trim().replace(/\s+/g, '') + '-' +
                   match[3].replace(/\s+/g, '');
          }
          return h.replace(/\s+/g, '-').replace(/\[|\]/g, '');
        });

      var response = UrlFetchApp.fetch('http://sensors.joinyourteam.it:8090/api/packets', {
        method: 'post',
        contentType: 'application/json',
        muteHttpExceptions: true,
        payload: JSON.stringify({
          projectId: 101,
          typeOfDevice: 'Device4G',
          macAddress: payload.macAddress, // o devEui
          payload: payload,
          spec: spec
        })
      });

      if (response.getResponseCode() === 200) {
        Logger.log('Packet accepted: ' + response.getContentText());
      } else {
        Logger.log('Packet rejected: ' +
                   response.getResponseCode() + ' ' +
                   response.getContentText());
      }
    } catch (errApi) {
      Logger.log('Errore chiamando AetherSense API: ' + errApi);
    }

    return ContentService.createTextOutput("Ok");
  } catch (err) {
    return ContentService.createTextOutput("Errore nello script: " + err);
  }
}

/** Crea o aggiorna la riga intestazioni */
function setHeaders(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(HEADERS);
    return;
  }
  ensureColumns(sheet, HEADERS.length);
  var range = sheet.getRange(1, 1, 1, HEADERS.length);
  var current = range.getValues()[0];
  var needsUpdate = false;
  for (var i = 0; i < HEADERS.length; i++) {
    if (!current[i] || current[i].toString().trim() === "") {
      needsUpdate = true;
      break;
    }
  }
  if (needsUpdate) range.setValues([HEADERS]);
}

/** Garantisce almeno N colonne disponibili */
function ensureColumns(sheet, minColumns) {
  var current = sheet.getMaxColumns();
  if (current < minColumns) {
    sheet.insertColumnsAfter(current, minColumns - current);
  }
}

function testEndpoint() {
  var url = 'http://sensors.joinyourteam.it:8090/api/packets';
  try {
    var res = UrlFetchApp.fetch(url, {method: 'get', muteHttpExceptions: true});
    Logger.log(res.getResponseCode());
    Logger.log(res.getContentText());
  } catch (err) {
    Logger.log('Fetch failed: ' + err);
  }
}
