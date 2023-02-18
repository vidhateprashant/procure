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
@Table(schema = "procure", name = "rtv_item")
@ToString
@Audited
@AuditTable("rtv_item_aud")
public class RtvItem implements Cloneable{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="rtv_id")
	private Long rtvId;
	
	@Column(name="rtv_number")
	private String rtvNumber;
	
	@Column(name="item_id")
	private Long itemId;
	
	@Column(name="poi_id")
	private Long poiId;
	
	private Long grnId;
	
	@Column(name="item_description")
	private String itemDescription;

	@Column(name="gl_code")
	private String glCode;
	
	@Column(name="recieved_quantity", precision=10, scale=2)
	private Double recievedQuantity;
	
	@Column(name="return_quantity", precision=10, scale=2)
	private Double returnQuantity;
	
	@Column(name="already_return_quantity", precision=10, scale=2)
	private Double alreadyReturnQuantity;
	
	@Column(name="rate", precision=10, scale=2)
	private Double rate;
	
	@Column(name="amount", precision=10, scale=2)
	private Double amount;
	
	private String department;
	
	@Column(name = "is_deleted", columnDefinition = "boolean default false")
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
	 * @param supplier
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<RtvHistory> compareFields(RtvItem rtvItem)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<RtvHistory> rtvHistories = new ArrayList<RtvHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(rtvItem);

				if (oldValue == null) {
					if (newValue != null) {
						rtvHistories.add(this.prepareRtvHistory(rtvItem, field));
					}
				} else if (!oldValue.equals(newValue)) {
					rtvHistories.add(this.prepareRtvHistory(rtvItem, field));
				}
			}
		}
		return rtvHistories;
	}
	
	private RtvHistory prepareRtvHistory(RtvItem rtvItem, Field field) throws IllegalAccessException {
		RtvHistory rtvHistory = new RtvHistory();
		rtvHistory.setRtvNumber(rtvItem.getRtvNumber());
		rtvHistory.setChildId(rtvItem.getId());
		rtvHistory.setModuleName(AppConstants.RTV_ITEM);
		rtvHistory.setChangeType(AppConstants.UI);
		rtvHistory.setLastModifiedBy(rtvItem.getLastModifiedBy());
		rtvHistory.setOperation(Operation.UPDATE.toString());
		rtvHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		rtvHistory.setOldValue(field.get(this).toString());
		rtvHistory.setNewValue(field.get(rtvItem).toString());
		return rtvHistory;
	}

}
