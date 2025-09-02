package it.sensorplatform.dto;

import java.util.Objects;


public class DeviceDTO {
	
	private String name;
	private String macAddress;
	private Double longitude;
	private Double latitude;
	private String emailOwner;
	private String devEui;
	private String tod;
	private String operator;
	
	public DeviceDTO(String name, String macAddress, String emailOwner, String devEui, Double longitude, Double latitude, String tod, String operator) {
		this.name = name;
		this.macAddress = macAddress;
		this.emailOwner=emailOwner;
		this.devEui=devEui;
		this.longitude = longitude;
		this.latitude = latitude;
		this.tod=tod;
		this.operator=operator;
	}

	public String getEmailOwner() {
		return emailOwner;
	}

	public void setEmailOwner(String emailOwner) {
		this.emailOwner = emailOwner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDevEui() {
		return devEui;
	}

	public void setDevEui(String devEui) {
		this.devEui = devEui;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public String getTod() {
		return tod;
	}

	public void setTod(String tod) {
		this.tod = tod;
	}
	

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(macAddress);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DeviceDTO other = (DeviceDTO) obj;
		return Objects.equals(macAddress, other.macAddress);
	}
	
	
	
	
}
