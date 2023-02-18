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
@Table(schema = "procure", name = "quotation_items")
@ToString
@Audited
@AuditTable("quotation_items_aud")
public class QuotationItem implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "quotation_id")
	private Long quotationId;

	@NotBlank(message = "RFQ Number is mandatory")
	@Column(name = "rfq_number")
	private String rfqNumber;
	
	@NotNull(message = "Item is mandatory")
	@Column(name = "item_id")
	private Long itemId;
	
	@Column(precision=10, scale=2)
	private Double quantity;
	
	private String currency;
	
	@Column(name = "received_date")
	private Date receivedDate;
	
	private String remarks;
	
	@Column(name = "pr_number")
	private String prNumber;
	
	private Long prId;
	
	@Column(name = "pr_location")
	private Long prLocation;
	
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
	
	@Transient
	private String itemDescription;
	
	@Transient
	private String itemUom;
	
	@Transient
	private String itemNameToDisplay;
	
	@Transient
	private String prLocationName;
	
	@Transient
	private Double remainedQuantity;
	
	@Transient
	private List<QuotationItemVendor> itemVendors;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotationItem
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<QuotationHistory> compareFields(QuotationItem quotationItem)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(quotationItem);

				if (oldValue == null) {
					if (newValue != null) {
						quotationHistories.add(this.prepareQuotationHistory(quotationItem, field));
					}
				} else if (!oldValue.equals(newValue)) {
					quotationHistories.add(this.prepareQuotationHistory(quotationItem, field));
				}
			}
		}
		return quotationHistories;
	}

	private QuotationHistory prepareQuotationHistory(QuotationItem quotationItem, Field field) throws IllegalAccessException {
		QuotationHistory quotationHistory = new QuotationHistory();
		quotationHistory.setRfqNumber(quotationItem.getRfqNumber());
		quotationHistory.setChildId(quotationItem.getId());
		quotationHistory.setModuleName(AppConstants.QUOTATION_ITEM);
		quotationHistory.setChangeType(AppConstants.UI);
		quotationHistory.setOperation(Operation.UPDATE.toString());
		quotationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			quotationHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(quotationItem) != null) {
			quotationHistory.setNewValue(field.get(quotationItem).toString());			
		}
		quotationHistory.setLastModifiedBy(quotationItem.getLastModifiedBy());
		return quotationHistory;
	}
	
	public QuotationItem(Long id, Long quotationId, String rfqNumber, Long itemId, String name, String description, String uom, Double quantity, Date receivedDate, String remarks,
			String prNumber, Long prLocation, String locationName, Long prId) {
		this.id = id;
		this.quotationId = quotationId;
		this.rfqNumber = rfqNumber;
		this.itemId = itemId;
		this.itemName = name;
		this.itemDescription = description;
		this.itemUom = uom;
		this.quantity = quantity;
		this.receivedDate = receivedDate;
		this.remarks = remarks;
		this.prNumber = prNumber;
		this.prLocation = prLocation;
		this.prLocationName = locationName; 
		this.prId = prId;
	}
	
	public QuotationItem(String rfqNumber, Long quotationId, Long itemId, String itemName, Double quantity, String uom, String prNumber, Long prLocationId, String locationName, Long prId) {
		this.rfqNumber = rfqNumber;
		this.quotationId = quotationId;
		this.itemId = itemId;
		this.itemName = itemName;
		this.itemUom = uom;
		this.itemNameToDisplay = itemName + "-" + prNumber + "-" + locationName;
		this.quantity = quantity;
		this.prNumber = prNumber;
		this.prLocation = prLocationId;
		this.prLocationName = locationName; 
		this.prId = prId;
	}
}
