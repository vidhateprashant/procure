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
@Table(schema = "procure", name = "quotation_vendors")
@ToString
@Audited
@AuditTable("quotation_vendors_aud")
public class QuotationVendors implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "quotation_id")
	private Long quotationId;

	@NotBlank(message = "RFQ Number is mandatory")
	@Column(name = "rfq_number")
	private String rfqNumber;
	
	@NotNull(message = "Vendor is mandatory")
	@Column(name = "vendor_id")
	private Long vendorId;
	
	@Column(name = "contact_name")
	private String contactName;
	
	private String email;
	
	private String memo;
	
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
	private String vendorName;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotationVendor
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<QuotationHistory> compareFields(QuotationVendors quotationVendor)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(quotationVendor);

				if (oldValue == null) {
					if (newValue != null) {
						quotationHistories.add(this.prepareQuotationHistory(quotationVendor, field));
					}
				} else if (!oldValue.equals(newValue)) {
					quotationHistories.add(this.prepareQuotationHistory(quotationVendor, field));
				}
			}
		}
		return quotationHistories;
	}

	private QuotationHistory prepareQuotationHistory(QuotationVendors quotationVendor, Field field) throws IllegalAccessException {
		QuotationHistory quotationHistory = new QuotationHistory();
		quotationHistory.setRfqNumber(quotationVendor.getRfqNumber());
		quotationHistory.setChildId(quotationVendor.getId());
		quotationHistory.setModuleName(AppConstants.QUOTATION_VENDOR);
		quotationHistory.setChangeType(AppConstants.UI);
		quotationHistory.setOperation(Operation.UPDATE.toString());
		quotationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			quotationHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(quotationVendor) != null) {
			quotationHistory.setNewValue(field.get(quotationVendor).toString());			
		}
		quotationHistory.setLastModifiedBy(quotationVendor.getLastModifiedBy());
		return quotationHistory;
	}
	
	public QuotationVendors(Long id, Long quotationId, String rfqNumber, Long vendorId, String name, String contactName, String email, String memo) {
		this.id = id;
		this.quotationId = quotationId;
		this.rfqNumber = rfqNumber;
		this.vendorId = vendorId;
		this.vendorName = name;
		this.contactName = contactName;
		this.email = email;
		this.memo = memo;
	}
}