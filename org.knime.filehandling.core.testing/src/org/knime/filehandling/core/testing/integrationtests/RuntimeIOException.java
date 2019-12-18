package org.knime.filehandling.core.testing.integrationtests;

import java.io.IOException;

/**
 * Wrapper class that creates an unchecked exception around an IOException.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class RuntimeIOException extends RuntimeException {
	
	private static final long serialVersionUID = 2865078972357499710L;
	private final String m_errorMessage;
	
	/**
	 * Creates a new RuntimeIOException.
	 * 
	 * @param errorMessage the error message
	 * @param e the IOException to wrap
	 */
	public RuntimeIOException(String errorMessage, IOException e) {
		super(errorMessage, e);
		m_errorMessage = errorMessage;
	}
	
	@Override
	public String getMessage() {
		return m_errorMessage + super.getMessage();
	}

}
