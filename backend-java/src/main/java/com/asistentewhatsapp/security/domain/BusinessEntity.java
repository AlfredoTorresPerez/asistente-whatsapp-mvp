package com.asistentewhatsapp.security.domain;

import com.asistentewhatsapp.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "business")
public class BusinessEntity extends AuditableEntity {

	@Id
	private UUID id;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "company_name", nullable = false)
	private String companyName;

	@Column(name = "business_name", nullable = false)
	private String businessName;

	@Column(name = "timezone", nullable = false)
	private String timezone;

	@Column(name = "currency", nullable = false)
	private String currency;

	@Column(name = "contact_email", nullable = false)
	private String contactEmail;

	@Column(name = "support_phone")
	private String supportPhone;

	@Column(name = "address")
	private String address;

	@Column(name = "active", nullable = false)
	private boolean active;

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getBusinessName() {
		return businessName;
	}

	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getSupportPhone() {
		return supportPhone;
	}

	public void setSupportPhone(String supportPhone) {
		this.supportPhone = supportPhone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isActive() {
		return active;
	}
}
