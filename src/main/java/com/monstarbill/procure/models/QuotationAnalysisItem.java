package com.monstarbill.procure.models;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
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
@Table(schema = "procure", name = "quotation_analysis_items")
@ToString
@Audited
@AuditTable("quotation_analysis_items_aud")
public class QuotationAnalysisItem implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "qa_id")
	private Long qaId;
	
	@NotBlank(message = "QA Number is mandatory")
	@Column(name = "qa_number")
	private String qaNumber; // Auto-generated from sequencing
	
	@Column(name = "processed_po", columnDefinition = "boolean default false")
	private boolean isProcessedPo;
	
	@Column(name = "awarded", columnDefinition = "boolean default false")
	private boolean isAwarded;
	
	@Column(name = "client_qa_number")
	private String clientQaNumber;

	@Column(name = "recieved_date")
	private OffsetDateTime recievedDate;

	@Column(name = "general_supplier")
	private String generalSupplier;

	@Column(name = "approved_supplier")
	private Long approvedSupplier;

	@Column(name = "item_id")
	private Long itemId;
	
//	@Column(name = "item_name")
//	private String itemName;

	private String currency;

	@Column(precision=10, scale=2)
	private Double quantity;

	private String uom;
	
	@Column(name = "exchange_rate")
	private Double exchangeRate;

	@Column(name = "rate_per_unit")
	private Double ratePerUnit;
	
	@Column(name = "actual_rate")
	private Double actualRate;

	@Column(name = "expected_date")
	private OffsetDateTime expectedDate;

	@Column(name = "lead_time")
	private Integer leadTime;
	
//	@Column(name = "pr_number")
//	private String prNumber;
	
	private Long prId;
	
	@Column(name = "pr_location_id")
	private Long prLocationId;

	@Column(name = "po_ref")
	private String poRef;
	
	private Long poId;
	
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
	private String prLocation;
	
	@Transient
	private String prNumber;
	
	@Transient
	private String itemName;
	
	@Transient
	private String itemDescription;
	
	@Transient
	private String integratedId;

	@Transient
	private Long accountCode;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotationAnalysisItem
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<QuotationAnalysisHistory> compareFields(QuotationAnalysisItem quotationAnalysisItem)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<QuotationAnalysisHistory> quotationHistories = new ArrayList<QuotationAnalysisHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(quotationAnalysisItem);

				if (oldValue == null) {
					if (newValue != null) {
						quotationHistories.add(this.prepareQuotationHistory(quotationAnalysisItem, field));
					}
				} else if (!oldValue.equals(newValue)) {
					quotationHistories.add(this.prepareQuotationHistory(quotationAnalysisItem, field));
				}
			}
		}
		return quotationHistories;
	}

	private QuotationAnalysisHistory prepareQuotationHistory(QuotationAnalysisItem quotationAnalysisItem, Field field) throws IllegalAccessException {
		QuotationAnalysisHistory quotationAnalysisHistory = new QuotationAnalysisHistory();
		quotationAnalysisHistory.setQaNumber(quotationAnalysisItem.getQaNumber());
		quotationAnalysisHistory.setChildId(quotationAnalysisItem.getId());
		quotationAnalysisHistory.setModuleName(AppConstants.QUOTATION_ANALYSIS_ITEM);
		quotationAnalysisHistory.setChangeType(AppConstants.UI);
		quotationAnalysisHistory.setOperation(Operation.UPDATE.toString());
		quotationAnalysisHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			quotationAnalysisHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(quotationAnalysisItem) != null) {
			quotationAnalysisHistory.setNewValue(field.get(quotationAnalysisItem).toString());			
		}
		quotationAnalysisHistory.setLastModifiedBy(quotationAnalysisItem.getLastModifiedBy());
		return quotationAnalysisHistory;
	}
	
	public QuotationAnalysisItem(Long id, String qaNumber, Long itemId, String name, String description, String integratedId, Long accountCode,  String uom, 
			Double quantity, Double ratePerUnit, Double actualRate, OffsetDateTime recievedDate, Long poId, OffsetDateTime expectedDate) {
		this.id = id;
		this.qaNumber = qaNumber;
		this.itemId = itemId;
		this.itemName = name;
		this.itemDescription = description;
		this.integratedId = integratedId;
		this.accountCode = accountCode;
		this.uom = uom;
		this.quantity = quantity;
		this.ratePerUnit = ratePerUnit;
		this.actualRate = actualRate;
		this.recievedDate = recievedDate;
		this.poId = poId;
		this.expectedDate = expectedDate;
	}
	
	public QuotationAnalysisItem(Long qaId, String qaNumber, Double quantity) {
		this.qaId = qaId;
		this.qaNumber = qaNumber;
		if (quantity != null) this.quantity = quantity;
	}
}
