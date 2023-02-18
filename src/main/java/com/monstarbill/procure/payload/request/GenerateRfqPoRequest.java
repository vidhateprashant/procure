package com.monstarbill.procure.payload.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRfqPoRequest {
	// list of selected PR's from UI to create RFQ / PO
	private List<RfqPoRequest> rfqPoRequests;
	// user will send yes/no - If Yes then create common PO else create separate
	private boolean isCreateCommonRfqPo;
	// module name to create i.e. RFQ/PO
	private String moduleName;
}
