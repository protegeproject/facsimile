package edu.stanford.bmir.facsimile.dbq.exception;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class MissingConfigurationFileException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	
	/**
	 * Constructor
	 * @see RuntimeException#RuntimeException()
	 */
	public MissingConfigurationFileException() {
		super();
	}
	
	
	/**
	 * Constructor
	 * @param s	Message
	 * @see RuntimeException#RuntimeException(String s)
	 */
	public MissingConfigurationFileException(String s) {
		super(s);
	}
	
	
	/**
	 * Constructor
	 * @param s	Message
	 * @param throwable	Throwable
	 * @see RuntimeException#RuntimeException(String s, Throwable throwable)
	 */
	public MissingConfigurationFileException(String s, Throwable throwable) {
		super(s, throwable);
	}
	
	
	/**
	 * Constructor
	 * @param throwable	Throwable
	 * @see RuntimeException#RuntimeException(Throwable throwable)
	 */
	public MissingConfigurationFileException(Throwable throwable) {
		super(throwable);
	}
}
