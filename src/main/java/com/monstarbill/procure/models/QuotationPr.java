package com.monstarbill.procure.models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

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
@Table(schema = "procure", name = "quotation_pr")
@ToString
@Audited
@AuditTable("quotation_pr_aud")
public class QuotationPr {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "quotation_id")
	private Long quotationId;
	
	@Column(name = "quotation_number")
	private String quotationNumber;
	
//	@Column(name = "pr_number")
//	private String prNumber;
	
	@Column(name = "pr_id")
	private Long prId;

	@Column(name = "pr_location_id")
	private Long prLocationId;

	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@CreatedBy
	@Column(name="created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@LastModifiedBy
	@Column(name="last_modified_by")
	private String lastModifiedBy;

	@Transient
	private String prNumber;
}