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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
@Table(schema = "procure", name = "pr_item")
@ToString
@Audited
@AuditTable("pr_item_aud")
public class PrItem implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Purchase Requisition is is mandatory")
	@Column(name = "pr_id", nullable = false, updatable = false)
	private Long prId;

	@NotBlank(message = "Purchase Requisition Number is mandatory")
	@Column(name = "pr_number", updatable = false)
	private String prNumber;

	@NotNull(message = "Item is mandatory")
	@Column(name = "item_id", nullable = false, updatable = false)
	private Long itemId;

	@Column(name = "item_description")
	private String itemDescription;
	
	@Column(precision=10, scale=2)
	private Double quantity;
	
	@Column(precision=10, scale=2)
	private Double remainedQuantity;

	private Double rate;
	
	private String memo;
	
	private String integratedId;

	@Column(name = "estimated_amount")
	private Double estimatedAmount;

	@Column(name = "received_date")
	private Date receivedDate;
	
//	@Column(name = "po_number")
//	private String poNumber;
	
	private Long poId;

//	private Long rfqId;
	
	@Column(name = "is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@CreationTimestamp
	@Column(name = "created_date", updatable = false)
	private Date createdDate;

	@Column(name = "created_by")
	private String createdBy;

	@UpdateTimestamp
	@Column(name = "last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name = "last_modified_by")
	private String lastModifiedBy;

	@Transient
	private String itemName;

	@Transient
	private String itemUom;
	
	@Transient
	private String department;
	
	@Transient
	private Long accountId;
	
	@Transient
	private Long prLocationId;
	
	@Transient
	private String prLocationName;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public PrItem(Long id, Long itemId, Long prId, String prNumber, String name, String description, String uom, Double quantity,
			Double rate, Double estimatedAmount, Date receivedDate, String memo, Double remainedQuantity) {
		this.id = id;
		this.itemId = itemId;
		this.prId = prId;
		this.prNumber = prNumber;
		this.itemName = name;
		this.itemDescription = description;
		this.itemUom = uom;
		this.quantity = quantity;
		this.rate = rate;
		this.estimatedAmount = estimatedAmount;
		this.receivedDate = receivedDate;
//		this.poId = poId;
		this.memo = memo;
		this.remainedQuantity = remainedQuantity;
	}

	public PrItem(Long prId, Long itemId, String name, String description, String integratedId, String uom, Double quantity,
			Date receivedDate, String prNumber, String department, Double rate, Long accountId, Long locationId, String prLocationName, Double remainedQuantity) {
		this.prId = prId;
		this.itemId = itemId;
		this.itemName = name;
		this.itemDescription = description;
		this.integratedId = integratedId;
		this.itemUom = uom;
		this.quantity = quantity;
		this.receivedDate = receivedDate;
		this.prNumber = prNumber;
		this.department = department;
		this.rate = rate;
		this.accountId = accountId;
		this.prLocationId = locationId;
		this.prLocationName = prLocationName;
		this.remainedQuantity = remainedQuantity;
	}
}
