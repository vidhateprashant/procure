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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "procure", name = "purchase_requisition")
@ToString
@Audited
@AuditTable("purchase_requisition_aud")
@EqualsAndHashCode
public class PurchaseRequisition implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "external_id")
	private String externalId;
	
	@Column(name = "pr_number", nullable = false, updatable = false)
	private String prNumber;

	@NotNull(message = "Subsidiary is mandatory")
	@Column(name = "subsidiary_id", nullable = false, updatable = false)
	private Long subsidiaryId;
	
	@NotBlank(message = "PR Type is mandatory")
	private String type;

	@NotNull(message = "Location is mandatory")
	@Column(name = "location_id")
	private Long locationId;

	@Column(name = "project_name")
	private String projectName;

	@Column(name = "pr_date")
	private Date prDate;

	@NotBlank(message = "Currency is mandatory")
	private String currency;

	private String requestor;
	
	private String priority;
	
	private String memo;
	
	private String usedFor;

	@Column(name = "exchange_rate")
	private String exchangeRate;

	@Column(name = "netsuite_id")
	private String netSuiteId;
	
	@Column(name = "rejected_comments")
	private String rejectedComments;
	
	@NotNull(message = "Depertment is mandatory")
	private String department;

	@Column(name = "pr_status")
	private String prStatus;

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
	private List<PrItem> prItems;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private String locationName;
	
	@Transient
	private boolean isRfqEnabled;
	
	@Transient
	private Double totalValue;

	@Transient
	private String approvedByName;
	
	@Transient
	private boolean hasError;
	
	@Transient
	private String partiallyProcessedFor;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param purchaseRequisition
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<PurchaseRequisitionHistory> compareFields(PurchaseRequisition purchaseRequisition)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<PurchaseRequisitionHistory> purchaseRequisitionHistories = new ArrayList<PurchaseRequisitionHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(purchaseRequisition);

				if (oldValue == null) {
					if (newValue != null) {
						purchaseRequisitionHistories.add(this.preparePurchaseRequisitionHistory(purchaseRequisition, field));
					}
				} else if (!oldValue.equals(newValue)) {
					purchaseRequisitionHistories.add(this.preparePurchaseRequisitionHistory(purchaseRequisition, field));
				}
			}
		}
		return purchaseRequisitionHistories;
	}

	private PurchaseRequisitionHistory preparePurchaseRequisitionHistory(PurchaseRequisition purchaseRequisition, Field field) throws IllegalAccessException {
		PurchaseRequisitionHistory purchaseRequisitionHistory = new PurchaseRequisitionHistory();
		purchaseRequisitionHistory.setPrNumber(purchaseRequisition.getPrNumber());
		purchaseRequisitionHistory.setModuleName(AppConstants.PR);
		purchaseRequisitionHistory.setChangeType(AppConstants.UI);
		purchaseRequisitionHistory.setOperation(Operation.UPDATE.toString());
		purchaseRequisitionHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) purchaseRequisitionHistory.setOldValue(field.get(this).toString());
		if (field.get(purchaseRequisition) != null) purchaseRequisitionHistory.setNewValue(field.get(purchaseRequisition).toString());
		purchaseRequisitionHistory.setLastModifiedBy(purchaseRequisition.getLastModifiedBy());
		return purchaseRequisitionHistory;
	}

	public PurchaseRequisition(Long id, String prNumber, Date prDate, String department, String requestor,
			Long subsidiaryId, String prStatus, Long locationId, String subsidiaryName, String locationName) {
		this.id = id;
		this.prNumber = prNumber;
		this.prDate = prDate;
		this.department = department;
		this.requestor = requestor;
		this.subsidiaryId = subsidiaryId;
		this.prStatus = prStatus;
		this.locationId = locationId;
		this.subsidiaryName = subsidiaryName;
		this.locationName = locationName;
	}

	public PurchaseRequisition(Long id, String prNumber, Long subsidiaryId, Date prDate, String requestor,
			String department, String prStatus) {

		this.id = id;
		this.prNumber = prNumber;
		this.subsidiaryId = subsidiaryId;
		this.prDate = prDate;
		this.requestor = requestor;
		this.department = department;
		this.prStatus = prStatus;
	}
	public PurchaseRequisition(Long id, Long subsidiaryId, String requestor, String prNumber, Date prDate,
			String department, String rejectedComments, String subsidiaryName, Long locationId, String locationName,
			String prStatus, String approvedBy, String nextApprover, String nextApproverRole, String approverByName) {
		this.id = id;
		this.subsidiaryId = subsidiaryId;
		this.requestor = requestor;
		this.prNumber = prNumber;
		this.prDate = prDate;
		this.department = department;
		this.rejectedComments = rejectedComments;
		this.subsidiaryName = subsidiaryName;
		this.locationId =  locationId;
		this.locationName = locationName;
		this.prStatus = prStatus;
		this.approvedBy = approvedBy;
		this.nextApprover = nextApprover;
		this.nextApproverRole = nextApproverRole;
		this.approvedByName = approverByName;
	}
	public PurchaseRequisition(Long id, String prNumber, Long subsidiaryId, String type, Long locationId,
			Date prDate, String currency, String rejectedComments, String prStatus,
			String subsidiaryName, String locationName, String usedFor) {
		this.id = id;
		this.prNumber = prNumber;
		this.subsidiaryId = subsidiaryId;
		this.type = type;
		this.locationId = locationId;
		this.prDate = prDate;
		this.currency = currency;
		this.rejectedComments = rejectedComments;
		this.prStatus = prStatus;
		this.subsidiaryName = subsidiaryName;
		this.locationName = locationName;
		this.usedFor = usedFor;
		this.partiallyProcessedFor = usedFor;
	}
}
