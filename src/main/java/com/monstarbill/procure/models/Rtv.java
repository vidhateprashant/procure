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
@Table(schema = "procure", name = "rtv")
@ToString
@Audited
@AuditTable("rtv_aud")
public class Rtv implements Cloneable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="rtv_number", unique = true)
	private String rtvNumber;
	
	@Column(name="subsidiary_id")
	private Long subsidiaryId;

	@Column(name="supplier_id")
	private Long supplierId;
	
	@Column(name="grn_number")
	private String grnNumber;
	
	private Long grnId;
	
	@Column(name="location_id")
	private Long locationId;

	@Column(name = "rtv_date")
	private Date rtvDate;
	
	@Column(name="invoice_number")
	private String invoiceNumber;
	
	@Column(name="approval_status")
	private String approvalStatus;
	
	@Column(name="mode_of_transport")
	private String modeOfTransport;
	
	@Column(name="vehicle_number")
	private String vehicleNumber;
	
	@Column(name="aw_number")
	private String awNumber;
	
	@Column(name="rejected_comments")
	private String rejectedComments;
	
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
	private List<RtvItem> rtvItems;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private String supplierName;
	
	@Transient
	private String locationName;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param supplier
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<RtvHistory> compareFields(Rtv rtv) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<RtvHistory> rtvHistories = new ArrayList<RtvHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(rtv);

				if (oldValue == null) {
					if (newValue != null) {
						rtvHistories.add(this.prepareRtvHistory(rtv, field));
					}
				} else if (!oldValue.equals(newValue)) {
					rtvHistories.add(this.prepareRtvHistory(rtv, field));
				}
			}
		}
		return rtvHistories;
	}
	
	private RtvHistory prepareRtvHistory(Rtv rtv, Field field) throws IllegalAccessException {
		RtvHistory rtvHistory = new RtvHistory();
		rtvHistory.setRtvNumber(rtv.getRtvNumber());
		rtvHistory.setModuleName(AppConstants.RTV);
		rtvHistory.setChangeType(AppConstants.UI);
		rtvHistory.setLastModifiedBy(rtv.getLastModifiedBy());
		rtvHistory.setOperation(Operation.UPDATE.toString());
		rtvHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if(field.get(this) != null) rtvHistory.setOldValue(field.get(this).toString());
		if(field.get(rtv) != null) rtvHistory.setNewValue(field.get(rtv).toString());		//null check
		return rtvHistory;
	}

	public Rtv(Long id, String subsidiaryName, Date createdDate, String rtvNumber, String supplierName, String grnNumber, Date rtvDate) {
		this.id = id;
		this.subsidiaryName = subsidiaryName;
		this.createdDate = createdDate;
		this.rtvNumber = rtvNumber;
		this.supplierName = supplierName;
		this.grnNumber = grnNumber;
		this.rtvDate = rtvDate;
	}
}
