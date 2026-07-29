package com.asistentewhatsapp.bookings.application;

public class AppointmentReminderEmailDTO {

	private String businessName;
	private String customerName;
	private String serviceName;
	private String appointmentDate;
	private String appointmentTime;
	private String professionalName;
	private String branchName;
	private String address;
	private String phone;
	private String email;
	private String locationUrl;
	private String helpUrl;
	private int minimumNoticeHours;

	public String getBusinessName() {
		return businessName;
	}
	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getAppointmentDate() {
		return appointmentDate;
	}
	public void setAppointmentDate(String appointmentDate) {
		this.appointmentDate = appointmentDate;
	}
	public String getAppointmentTime() {
		return appointmentTime;
	}
	public void setAppointmentTime(String appointmentTime) {
		this.appointmentTime = appointmentTime;
	}
	public String getProfessionalName() {
		return professionalName;
	}
	public void setProfessionalName(String professionalName) {
		this.professionalName = professionalName;
	}
	public String getBranchName() {
		return branchName;
	}
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getLocationUrl() {
		return locationUrl;
	}
	public void setLocationUrl(String locationUrl) {
		this.locationUrl = locationUrl;
	}
	public String getHelpUrl() {
		return helpUrl;
	}
	public void setHelpUrl(String helpUrl) {
		this.helpUrl = helpUrl;
	}
	public int getMinimumNoticeHours() {
		return minimumNoticeHours;
	}
	public void setMinimumNoticeHours(int minimumNoticeHours) {
		this.minimumNoticeHours = minimumNoticeHours;
	}
}
