package com.monstarbill.procure.models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "setup", name = "supplier_contact")
@ToString
@Audited
@AuditTable("supplier_contact_aud")
@EqualsAndHashCode
public class SupplierContact implements Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="supplier_id")
	private Long supplierId;

	@NotBlank(message = " Name is mandatory")
	private String name;
	
	@NotBlank(message = "contact number is mandatory")
	@Column(name="contact_number")
	private String contactNumber;

	@Column(name="alt_contact_number")
	private String altContactNumber;

	private String email;

	private String web;

	private String fax;
	
	@Column(name="is_primary_contact", columnDefinition = "boolean default false")
	private boolean isPrimaryContact;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name="last_modified_by")
	private String lastModifiedBy;
	
}
