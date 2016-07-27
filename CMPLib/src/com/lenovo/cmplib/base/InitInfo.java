package com.lenovo.cmplib.base;

public class InitInfo {
	private EConnectorType connector;
	private ERoleType role;
	
	public enum EConnectorType
	{
		BLUETOOTH,
		SOFTAP
	};

	public enum ERoleType
	{
		MASTER,
		SLAVE
	}
	
	public InitInfo(){
		
	}
	
	public InitInfo(EConnectorType connector, ERoleType role){
		this.connector = connector;
		this.role = role;
	}

	public EConnectorType getConnector() {
		return connector;
	}

	public void setConnector(EConnectorType connector) {
		this.connector = connector;
	}

	public ERoleType getRole() {
		return role;
	}

	public void setRole(ERoleType role) {
		this.role = role;
	}
	
	
}
