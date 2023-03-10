package com.monstarbill.procure.enums;

/**
 * Transaction Status of the application
 * @author Prashant
 * 26-07-2022
 */
public enum TransactionStatus {

	// enum fields
	OPEN("Open"),
	DRAFT("Draft"),
	SUBMITTED("Submitted"),
	CLOSE("Close"),
	PROCESS("Process"),
	PARTIALLY_PROCESSED("Partially Processed"),
	PROCESSED("Processed"),
	PENDING_APPROVAL("Pending Approval"),
	QA_CREATED("QA Created"),
	APPROVED("Approved"),
	REJECTED("Rejected"),
	PARTIALLY_APPROVED("Partially Approved"),
	RETURN("Returned"),
	PARTIALLY_RETURN("Partially Returned"),
	FULLY_RETURNED("Fully Returned"),
	VOID("Voided"),
	BILLED("Billed"),
	PARTIALLY_PAID("Partially Paid"),
	PAID("Paid"),
	PARTIALLY_RECEIVED("Partially Recevied"),
	RECEIVED("Received"),
	PARTIALLY_APPLIED("Partially applied"),
	APPLIED("Applied"),
	PARTIALLY_BILLED("Partially Billed");
	
	// constructor
    private TransactionStatus(final String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
 
    // internal state
    private String transactionStatus;
 
    public String getTransactionStatus() {
        return transactionStatus;
    }
    
}
