package com.monstarbill.procure.enums;

import java.util.Arrays;

/**
 * Purchase order type
 * @author Prashant
 * 06-08-2022
 */
public enum PoType {

	PR_BASED("PR Based"),
	QA_BASED("QA Based"),
	STANDALONE("Standalone Purchase Order");
	
    private PoType(final String poType) {
        this.poType = poType;
    }
 
    private String poType;
 
    public String getPoType() {
        return poType;
    }
    
    public static PoType findByAbbr(final String abbr){
        return Arrays.stream(values()).filter(value -> value.getPoType().equals(abbr)).findFirst().orElse(null);
    }
    
}
