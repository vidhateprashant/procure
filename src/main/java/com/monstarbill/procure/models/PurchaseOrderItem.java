package com.monstarbill.procure.models;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

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
@Table(schema = "procure", name = "purchase_order_item")
@ToString
@Audited
@AuditTable("purchase_order_item_aud")
public class PurchaseOrderItem implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "PO Number is mandatory")
	@Column(name = "po_number", nullable = false)
	private String poNumber;
	
	@Column(name = "po_id")
	private Long poId;
	
	@Column(name = "item_id")
	private Long itemId;
	
	@Column(name = "item_description")
	private String itemDescription;
	
	@Column(precision=10, scale=2)
	private Double quantity;

	@Column(precision=10, scale=2)
	private Double rate;

	@Column(precision=10, scale=2)
	private Double amount;
	
	@Column(precision=10, scale=2)
	private Double remainQuantity;

	@Column(name = "tax_group_id")
	private Long taxGroupId;

	@Column(name = "tax_amount", precision=10, scale=2)
	private Double taxAmount;
	
	@Column(name = "total_tax_amount", precision=10, scale=2)
	private Double totalTaxAmount;
	
	@Column(name = "total_amount", precision=10, scale=2)
	private Double totalAmount;

	@Column(name = "received_by_date")
	private Date receivedByDate;
	
	@Column(name = "unbilled_quantity", precision=10, scale=2)
	private Double unbilledQuantity;

//	@Column(name = "pr_number")
//	private String prNumber;
	
	private Long prId;

	@Column(name = "ship_to_location_id")
	private Long shipToLocationId;
	
	@Column(name = "ship_to_location")
	private String shipToLocation;

	private String department;

	private String memo;
	
	private String status;
	
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
	private String itemName;

//	@Transient
//	private String itemDescription;

	@Transient
	private Long accountId;
	
	@Transient
	private String accountCode;
	
	@Transient
	private String itemUom;

	@Transient
	private String itemIntegratedId;
	
	@Transient
	private Long qaId;
	
	@Transient
	private String prNumber;
	
	@Transient
	private Double prItemRemainedQuantity;
	
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
	public List<PurchaseOrderHistory> compareFields(PurchaseOrderItem purchaseOrder)
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

	private PurchaseOrderHistory preparePurchaseOrderHistory(PurchaseOrderItem purchaseOrder, Field field) throws IllegalAccessException {
		PurchaseOrderHistory poHistory = new PurchaseOrderHistory();
		poHistory.setPoNumber(purchaseOrder.getPoNumber());
		poHistory.setChildId(purchaseOrder.getId());
		poHistory.setModuleName(AppConstants.PURCHASE_ORDER_ITEM);
		poHistory.setChangeType(AppConstants.UI);
		poHistory.setOperation(Operation.UPDATE.toString());
		poHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) poHistory.setOldValue(field.get(this).toString());
		if (field.get(purchaseOrder) != null) poHistory.setNewValue(field.get(purchaseOrder).toString());
		poHistory.setLastModifiedBy(purchaseOrder.getLastModifiedBy());
		return poHistory;
	}
	
	// constructor used for single get call
	public PurchaseOrderItem(Long id, String poNumber, Long poId, Long itemId, Double quantity, Double rate, Long accountId, Double amount, Long taxGroupId, Double taxAmount, Date receivedByDate, Long prId, 
			Long shipToLocationId, String shipToLocation, String department, String memo, String itemName, String itemDescription, String itemUom, String itemIntegratedId, String prNumber, Double remainQuantity,
			Double prItemRemainedQuantity) {
		this.id = id;
		this.poId = poId;
		this.poNumber = poNumber;
		this.itemId = itemId;
		this.quantity = quantity;
		this.rate = rate;
		this.accountId = accountId;
		this.amount = amount;
		this.taxGroupId = taxGroupId;
		this.taxAmount = taxAmount;
		this.receivedByDate = receivedByDate;
		this.prId = prId;
		this.shipToLocationId = shipToLocationId;
		this.shipToLocation = shipToLocation;
		this.department = department;
		this.memo = memo;
		this.itemName = itemName;
		this.itemDescription = itemDescription;
		this.itemUom = itemUom;
		this.itemIntegratedId = itemIntegratedId;
		this.prNumber = prNumber;
		this.remainQuantity = remainQuantity;
		this.prItemRemainedQuantity = prItemRemainedQuantity;
	}

	public PurchaseOrderItem(Long id, String poNumber,Long taxGroupId, Long poId, Long itemId, Double remainQuantity, Double quantity, Double rate,
			String itemName, String itemDescription, String itemUom) {
		this.id = id;
		this.poNumber = poNumber;
		this.taxGroupId = taxGroupId;
		this.poId = poId;
		this.itemId = itemId;
		this.remainQuantity = remainQuantity;
		this.quantity = quantity;
		this.rate = rate;
		this.itemName = itemName;
		this.itemDescription = itemDescription;
		this.itemUom = itemUom;
	}
	
	public PurchaseOrderItem(Long id, String poNumber, Long qaId, Double quantity) {
		this.poId = id;
		this.poNumber = poNumber;
		this.qaId = qaId;
		this.quantity = quantity;
	}
	
	
}
