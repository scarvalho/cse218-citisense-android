package org.sosa.richservice.base;

import java.io.Serializable;

import org.sosa.richservice.MessageRequest;

/**
 * 
 * @author celal.ziftci
 * 
 */
@SuppressWarnings("unchecked")
public class MessageRequestBase extends MessageBase implements MessageRequest,
		Serializable {
	private static final long serialVersionUID = -732762348720345971L;

	private final String operation;

	private final Class[] operationParameterTypes;

	private final Object[] operationParameterValues;

	public MessageRequestBase(String source, String destination,
			String messageId, String operation,
			Class[] operationParameterTypes, Object[] operationParameterValues) {
		super(source, destination, messageId);
		this.operation = operation;
		this.operationParameterTypes = operationParameterTypes;
		this.operationParameterValues = operationParameterValues;
	}

	@Override
	public String getOperation() {
		return operation;
	}

	@Override
	public Class[] getOperationParameterTypes() {
		return operationParameterTypes;
	}

	@Override
	public Object[] getOperationParameterValues() {
		return operationParameterValues;
	}

}
