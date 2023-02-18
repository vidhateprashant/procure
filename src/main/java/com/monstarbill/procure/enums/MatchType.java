package com.monstarbill.procure.enums;

import java.util.Arrays;

/**
 * Match type of PO
 * @author Prashant
 * 06-08-2022
 */
public enum MatchType {

	WAY_2("2 Way"),
	WAY_3("3 Way");
	
    private MatchType(final String matchType) {
        this.matchType = matchType;
    }
 
    private String matchType;
 
    public String getMatchType() {
        return matchType;
    }
    
    public static MatchType findByAbbr(final String abbr){
        return Arrays.stream(values()).filter(value -> value.getMatchType().equals(abbr)).findFirst().orElse(null);
    }
    
}
