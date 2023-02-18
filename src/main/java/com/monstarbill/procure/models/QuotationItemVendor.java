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
@Table(schema = "procure", name = "quotation_item_vendors")
@ToString
@Audited
@AuditTable("quotation_item_vendors_aud")
public class QuotationItemVendor implements Cloneable {

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
	
	@Column(name = "vendor_id")
	private Long vendorId;
	
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
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotationItemVendor
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
//	public List<QuotationHistory> compareFields(QuotationItemVendor quotationItemVendor)
//			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//
//		List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
//		Field[] fields = this.getClass().getDeclaredFields();
//
//		for (Field field : fields) {
//			String fieldName = field.getName();
//
//			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
//				Object oldValue = field.get(this);
//				Object newValue = field.get(quotationItemVendor);
//
//				if (oldValue == null) {
//					if (newValue != null) {
//						quotationHistories.add(this.prepareQuotationHistory(quotationItemVendor, field));
//					}
//				} else if (!oldValue.equals(newValue)) {
//					quotationHistories.add(this.prepareQuotationHistory(quotationItemVendor, field));
//				}
//			}
//		}
//		return quotationHistories;
//	}
//
//	private QuotationHistory prepareQuotationHistory(QuotationItemVendor quotationItemVendor, Field field) throws IllegalAccessException {
//		QuotationHistory quotationHistory = new QuotationHistory();
//		quotationHistory.setRfqNumber(quotationItemVendor.getRfqNumber());
//		quotationHistory.setChildId(quotationItemVendor.getId());
//		quotationHistory.setModuleName(AppConstants.QUOTATION_ITEM_VENDOR);
//		quotationHistory.setChangeType(AppConstants.UI);
//		quotationHistory.setOperation(Operation.UPDATE.toString());
//		quotationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
//		if (field.get(this) != null) quotationHistory.setOldValue(field.get(this).toString());
//		if (field.get(quotationItemVendor) != null) quotationHistory.setNewValue(field.get(quotationItemVendor).toString());
//		quotationHistory.setLastModifiedBy(quotationItemVendor.getLastModifiedBy());
//		return quotationHistory;
//	}
}