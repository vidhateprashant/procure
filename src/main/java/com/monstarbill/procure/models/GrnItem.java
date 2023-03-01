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
@Table(schema = "procure", name = "grn_item")
@ToString
@Audited
@AuditTable("grn_item_aud")
public class GrnItem implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "grn_id")
	private Long grnId;
	
	@Column(name = "po_id")
	private Long poId;
	
	private Long poiId;
	
	@Column(name = "item_id")
	private Long itemId;
	
	@Column(name = "invoice_id")
	private Long invoiceId;
	
	@Column(name = "po_number")
	private String poNumber;
	
	@Column(name = "grn_number")
	private String grnNumber;
	
	private String itemName;
	
	private String status;
	
	private String itemDescription;
	
	private String itemUom;
	
	@Column(name = "tax_group_id")
	private Long taxGroupId;
	
	@Column(precision=10, scale=2)
	private Double quantity;
	
	@Column(precision=10, scale=2)
	private Double reciveQuantity;
	
	@Column(precision=10, scale=2)
	private Double remainQuantity;
	
	@Column(precision=10, scale=2)
	private Double unbilledQuantity;
	
	
	@Column(name = "lot_number")
	private String lotNumber;
	
	@Column(name = "rate", precision=10, scale=2)
	private Double rate;
	
	@Column(name = "rtv_quantity", precision=10, scale=2)
	private Double rtvQuantity;
	
	@Column(name = "invoice_number")
	private String invoiceNumber;
	
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
	 * @param grnItem
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<GrnHistory> compareFields(GrnItem grnItem)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<GrnHistory> grnHistories = new ArrayList<GrnHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(grnItem);

				if (oldValue == null) {
					if (newValue != null) {
						grnHistories.add(this.prepareGrnHistory(grnItem, field));
					}
				} else if (!oldValue.equals(newValue)) {
					grnHistories.add(this.prepareGrnHistory(grnItem, field));
				}
			}
		}
		return grnHistories;
	}

	private GrnHistory prepareGrnHistory(GrnItem grnItem, Field field) throws IllegalAccessException {
		GrnHistory grnHistory = new GrnHistory();
		grnHistory.setGrnNumber(grnItem.getGrnNumber());
		grnHistory.setChildId(grnItem.getId());
		grnHistory.setModuleName("GRN Item");
		grnHistory.setChangeType(AppConstants.UI);
		grnHistory.setOperation(Operation.UPDATE.toString());
		grnHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) grnHistory.setOldValue(field.get(this).toString());
		if (field.get(grnItem) != null) grnHistory.setNewValue(field.get(grnItem).toString());
		grnHistory.setLastModifiedBy(grnItem.getLastModifiedBy());
		return grnHistory;
	}

	public GrnItem(Long id, Long grnId, Long itemId, String grnNumber, Double reciveQuantity,
			Double remainQuantity, String lotNumber, Double rate, Double rtvQuantity, String invoiceNumber,
			String itemName, String itemDescription, String itemUom, Double quantity, Long taxGroupId) {
		this.id = id;
		this.grnId = grnId;
		this.itemId = itemId;
		this.grnNumber = grnNumber;
		this.reciveQuantity = reciveQuantity;
		this.remainQuantity = remainQuantity;
		this.lotNumber = lotNumber;
		this.rate = rate;
		this.rtvQuantity = rtvQuantity;
		this.invoiceNumber = invoiceNumber;
		this.itemName = itemName;
		this.itemDescription = itemDescription;
		this.itemUom = itemUom;
		this.quantity = quantity;
		this.taxGroupId = taxGroupId;
	}

	
}
