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

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

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
@Table(schema = "procure", name = "quotation_general_info")
@ToString
@Audited
@AuditTable("quotation_general_info_aud")
public class QuotationGenaralInfo implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "quotation_id")
	private Long quotationId;
	
	@Column(name = "quotation_number")
	private String quotationNumber;
	
	private String remarks;

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
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotationInfo
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<QuotationHistory> compareFields(QuotationGenaralInfo quotationInfo)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(quotationInfo);

				if (oldValue == null) {
					if (newValue != null) {
						quotationHistories.add(this.prepareQuotationInfoHistory(quotationInfo, field));
					}
				} else if (!oldValue.equals(newValue)) {
					quotationHistories.add(this.prepareQuotationInfoHistory(quotationInfo, field));
				}
			}
		}
		return quotationHistories;
	}

	private QuotationHistory prepareQuotationInfoHistory(QuotationGenaralInfo quotationInfo, Field field) throws IllegalAccessException {
		QuotationHistory quotationHistory = new QuotationHistory();
		quotationHistory.setChildId(quotationInfo.getId());
		quotationHistory.setRfqNumber(quotationInfo.getQuotationNumber());
		quotationHistory.setModuleName(AppConstants.QUOTATION_GENERAL_INFO);
		quotationHistory.setChangeType(AppConstants.UI);
		quotationHistory.setOperation(Operation.UPDATE.toString());
		quotationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			quotationHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(quotationInfo) != null) {
			quotationHistory.setNewValue(field.get(quotationInfo).toString());
		}
		quotationHistory.setLastModifiedBy(quotationInfo.getLastModifiedBy());
		return quotationHistory;
	}
}