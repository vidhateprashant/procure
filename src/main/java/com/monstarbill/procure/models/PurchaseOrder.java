package com.monstarbill.procure.models;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

import com.monstarbill.procure.commons.AppConstants;
import com.monstarbill.procure.commons.CommonUtils;
import com.monstarbill.procure.enums.Operation;

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
@Table(schema = "procure", name = "purchase_order")
@ToString
@Audited
@AuditTable("purchase_order_aud")
public class PurchaseOrder implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "external_id")
	private String externalId;

	@Column(name = "po_number", nullable = false, unique = true)
	private String poNumber;
	
	@NotNull(message = "Subsidiary is mandatory")
	@Column(name = "subsidiary_id", nullable = false)
	private Long subsidiaryId;
	
	@NotBlank(message = "PO type is mandatory")
	@Column(name = "po_type")
	private String poType;
	
	@Column(name = "location_id")
	private Long locationId;
	
	@Column(name = "location")
	private String location;

//	@Column(name = "pr_number")
//	private String prNumber;
	
	private String prId;

//	@Column(name = "qa_number")
//	private String qaNumber;
	
	private Long qaId;
	
	@NotNull(message = "Supplier is mandatory")
	@Column(name = "supplier_id")
	private Long supplierId;

	@Column(precision = 10, scale = 2)
	private Double amount;

	@Column(precision = 10, scale = 2)
	private Double taxAmount;

	@Column(precision = 10, scale = 2)
	private Double totalAmount;

	@Column(name = "original_supplier_id")
	private Long originalSupplierId;
	
	@Column(name="is_supplier_updatable", columnDefinition = "boolean default false")
	private boolean isSupplierUpdatable;

	@NotNull(message = "PO date is mandatory")
	@Column(name = "po_date")
	private Date poDate;

	@Column(name = "rejected_comments")
	private String rejectedComments;

	@Column(name = "payment_term")
	private String paymentTerm;

	@NotBlank(message = "Match type is mandatory")
	@Column(name = "match_type")
	private String matchType;

	@NotBlank(message = "Currency is mandatory")
	private String currency;

	@Column(name = "exchange_rate")
	private Double exchangeRate;

	@Column(name = "po_status")
	private String poStatus;

	private String memo;
	
	@Column(name = "bill_to")
	private Long billTo;
	
	@Column(name = "ship_to")
	private Long shipTo;
	
	@Column(name = "trn")
	private String trn;

	@Column(name = "netsuite_id")
	private String netsuiteId;
	
	@Column(name = "approved_by")
	private String approvedBy;
	
	@Column(name = "next_approver")
	private String nextApprover;
	
	@Column(name = "next_approver_role")
	private String nextApproverRole;

	// stores the next approver level i.e. L1,L2,L3 etc.
	@Column(name = "next_approver_level")
	private String nextApproverLevel;
	
	// store's the id of approver preference
	@Column(name = "approver_preference_id")
	private Long approverPreferenceId;
	
	// stores the approver sequence id (useful internally in order to change the approver)
	@Column(name = "approver_sequence_id")
	private Long approverSequenceId;
	
	// stores the max level to approve, after that change status to approve
	@Column(name = "approver_max_level")
	private String approverMaxLevel;
	
	@Column(name = "note_to_approver")
	private String noteToApprover;
	
	@Transient
	private boolean isApprovalRoutingActive;
	
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
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private String supplierName;

	@Transient
	private String locationName;

	@Transient
	private boolean hasError;

	@Transient
	private Double totalValue;

	@Transient
	private List<PurchaseOrderItem> purchaseOrderItems;
	
	@Transient
	private int revision;
	
	@Transient
	private String approvedByName;
	
	@Transient
	private String qaNumber;
	
	@Transient
	private Map<Long, String> prNumbers;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param purchaseOrder
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<PurchaseOrderHistory> compareFields(PurchaseOrder purchaseOrder)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<PurchaseOrderHistory> poHistories = new ArrayList<PurchaseOrderHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(purchaseOrder);

				if (oldValue == null) {
					if (newValue != null) {
						poHistories.add(this.preparePurchaseOrderHistory(purchaseOrder, field));
					}
				} else if (!oldValue.equals(newValue)) {
					poHistories.add(this.preparePurchaseOrderHistory(purchaseOrder, field));
				}
			}
		}
		return poHistories;
	}

	private PurchaseOrderHistory preparePurchaseOrderHistory(PurchaseOrder purchaseOrder, Field field) throws IllegalAccessException {
		PurchaseOrderHistory poHistory = new PurchaseOrderHistory();
		poHistory.setPoNumber(purchaseOrder.getPoNumber());
		poHistory.setModuleName(AppConstants.PURCHASE_ORDER);
		poHistory.setChangeType(AppConstants.UI);
		poHistory.setOperation(Operation.UPDATE.toString());
		poHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) poHistory.setOldValue(field.get(this).toString());
		if (field.get(purchaseOrder) != null) poHistory.setNewValue(field.get(purchaseOrder).toString());
		poHistory.setLastModifiedBy(purchaseOrder.getLastModifiedBy());
		return poHistory;
	}


	
	public PurchaseOrder(Long id, String poNumber, Long subsidiaryId, Double totalAmount, Long locationId, String location,
			Long supplierId, Date poDate, String rejectedComments, String poStatus, String memo,
			String subsidiaryName, String locationName, String supplierName, String approvedBy,
			String nextApprover, String nextApproverRole, String approvedByName) {
		this.id = id;
		this.poNumber = poNumber;
		this.subsidiaryId = subsidiaryId;
		this.totalAmount = totalAmount;
		this.locationId = locationId;
		this.location = location;
		this.supplierId = supplierId;
		this.poDate = poDate;
		this.rejectedComments = rejectedComments;
		this.poStatus = poStatus;
		this.memo = memo;
		this.subsidiaryName = subsidiaryName;
		this.locationName = locationName;
		this.supplierName = supplierName;
		this.approvedBy =  approvedBy;
		this.nextApprover = nextApprover;
		this.nextApproverRole = nextApproverRole;
		this.approvedByName =  approvedByName;
		
	}

	public PurchaseOrder(Long id, String poNumber,String poStatus, Date poDate ) {
		this.id = id;
		this.poNumber = poNumber;
		this.poStatus = poStatus;
		this.poDate =  poDate;
	}

	public PurchaseOrder(Long id,String poNumber, Long supplierId, String currency, String supplierName) {
		this.id = id;
		this.poNumber = poNumber;
		this.supplierId = supplierId;
		this.currency = currency;
		this.supplierName = supplierName;
	}

	public PurchaseOrder(Long id, Long subsidiaryId, Long locationId, String locationName) {
		this.id = id;
		this.subsidiaryId = subsidiaryId;
		this.locationId = locationId;
		this.locationName = locationName;
	}

	public PurchaseOrder(Long id, String poNumber, Long subsidiaryId, String poType, Long supplierId,
			Double totalAmount, Date poDate, String currency, String subsidiaryName, String supplierName, String poStatus) {
		this.id = id;
		this.poNumber = poNumber;
		this.subsidiaryId = subsidiaryId;
		this.poType = poType;
		this.supplierId = supplierId;
		this.totalAmount = totalAmount;
		this.poDate = poDate;
		this.currency = currency;
		this.subsidiaryName = subsidiaryName;
		this.supplierName = supplierName;
		this.poStatus = poStatus;
	}

}
