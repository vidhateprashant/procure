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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "setup", name = "account")
@ToString
@Audited
@AuditTable("account_aud")
public class Account implements Cloneable{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Account Code is mandatory")
	@Column(name="code",nullable = false, updatable = false, unique = true)
	private String code;

	@NotBlank(message = "Account Description  is mandatory")
	@Column(name="description", nullable = false)
	private String description;

	@Column(name="parent")
	private Long parent;

	@NotBlank(message = "Account Type is mandatory")
	@Column(name="type")
	private String type;
	
	private String currency;

	@Column(name="is_inactive", columnDefinition = "boolean default false")
	private boolean isInactive;
	
	@Column(name="inactive_date")
	private Date inactiveDate;
	
	@Column(name="account_summary", columnDefinition = "boolean default false")
	private boolean isAccountSummary;
	
	@Column(name="tds_tax_code")
	private String tdsTaxCode;
	
	@Column(name="tax_code")
	private String taxCode;
	
//	@Column(name="restrict_cost_centre")
//	private String restrictCostCentre;
//	
//	@Column(name="subsidiary_id")
//	private Long subsidiaryId;
//	
//	@Column(name="restrict_department")
//	private String restrictDepartment;

	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by")
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name="last_modified_by")
	private String lastModifiedBy;
}
