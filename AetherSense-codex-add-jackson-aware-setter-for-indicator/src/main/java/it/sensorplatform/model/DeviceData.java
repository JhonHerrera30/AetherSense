package it.sensorplatform.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class DeviceData {	

	private Long id;  //Potenzialmete Classe Da Cancellare
	
	private Device device;
	
	private List <TypeOfDevice> types;
	
	private Map <LocalDateTime, List <MeasurementRecord>> db;

	
	public DeviceData() {
		this.db = new HashMap<>();
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public List<TypeOfDevice> getTypes() {
		return types;
	}

	public void setTypes(List<TypeOfDevice> types) {
		this.types = types;
	}
	
	

	public Map<LocalDateTime, List<MeasurementRecord>> getDb() {
		return db;
	}

	public void setDb(Map<LocalDateTime, List<MeasurementRecord>> db) {
		this.db = db;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeviceData other = (DeviceData) obj;
		return Objects.equals(id, other.id);
	}
	
	/*public void updateDb() {
		for(TypeOfMeasurement t : this.types) {
			if(this.db.containsKey(t.getCurrentRecord().getDateTime())) {
				this.db.get(t.getCurrentRecord().getDateTime()).add(t.getCurrentRecord());
			}
			else {
				List <MeasurementRecord> nuova = new ArrayList<>();
				nuova.add(t.getCurrentRecord());
				this.db.put(t.getCurrentRecord().getDateTime(), nuova);
			}
		}
	}*/
	
}
