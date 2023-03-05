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
import com.monstarbill.procure.enums.FormNames;
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
@Table(schema = "procure", name = "grn")
@ToString
@Audited
@AuditTable("grn_aud")
public class Grn implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "GRN number is mandatory")
	@Column(name = "grn_number", unique = true)
	private String grnNumber;

	@NotNull(message = "Subsidiary id is mandatory")
	@Column(name = "subsidiaryId")
	private Long subsidiaryId;

	@NotNull(message = "Location id is mandatory")
	@Column(name = "location_id")
	private Long locationId;

	@NotBlank(message = "PO number is mandatory")
	@Column(name = "po_number")
	private String poNumber;

	@NotNull(message = "GRN date is mandatory")
	@Column(name = "grn_date")
	private Date grnDate;

	@NotNull(message = "Supplier Id is mandatory")
	@Column(name = "supplier_id")
	private Long supplierId;

	@Column(name = "currency")
	private String currency;
	
	private String memo;
	
	@Column(name = "purchase_date")
	private Date purchaseDate;

	@Column(name = "exchange_rate")
	private Double exchangeRate;

	@Column(name = "mode_of_transport")
	private String modeOfTransport;

	@Column(name = "vehicle_number")
	private String vehicleNumber;

	@Column(name = "aw_number")
	private String awNumber;

	@Column(name = "e_way_bill_number")
	private String eWayBillNumber;

	@Column(name = "reciver")
	private String reciver;
	
	private String status;
	
	private String billStatus;
	
	private String rtvStatus;

	@Column(name = "netsuite_id")
	private String netsuiteId;

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
	
	private Long poId;

	@Transient
	private String locationName;

	@Transient
	private String subsidiaryName;
	
	@Transient
	private String supplierName;
	
	@Transient
	private List<GrnItem> grnItem;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Grn(Long id, String grnNumber, Long subsidiaryId, Long locationId, String poNumber, Date grnDate,
			Long supplierId, String subsidiaryName, String locationName, String supplierName) {
		this.id = id;
		this.grnNumber = grnNumber;
		this.subsidiaryId = subsidiaryId;
		this.locationId = locationId;
		this.poNumber = poNumber;
		this.grnDate = grnDate;
		this.supplierId = supplierId;
		this.subsidiaryName = subsidiaryName;
		this.locationName = locationName;
		this.supplierName = supplierName;			
	}
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param grn
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<GrnHistory> compareFields(Grn grn)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<GrnHistory> grnHistories = new ArrayList<GrnHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(grn);

				if (oldValue == null) {
					if (newValue != null) {
						grnHistories.add(this.prepareGrnHistory(grn, field));
					}
				} else if (!oldValue.equals(newValue)) {
					grnHistories.add(this.prepareGrnHistory(grn, field));
				}
			}
		}
		return grnHistories;
	}

	private GrnHistory prepareGrnHistory(Grn grn, Field field)
			throws IllegalAccessException {
		GrnHistory grnHistory = new GrnHistory();
		grnHistory.setGrnNumber(grn.getGrnNumber());
		grnHistory.setModuleName(FormNames.GRN.getFormName());
		grnHistory.setChangeType(AppConstants.UI);
		grnHistory.setOperation(Operation.UPDATE.toString());
		grnHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null)
			grnHistory.setOldValue(field.get(this).toString());
		if (field.get(grn) != null)
			grnHistory.setNewValue(field.get(grn).toString());
		grnHistory.setLastModifiedBy(grn.getLastModifiedBy());
		return grnHistory;
	}

}
