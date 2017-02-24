package us.shiroyama.android.vulture.processor.exceptions;

import javax.lang.model.element.Element;

/**
 * @author Fumihiko Shiroyama
 */

public class ProcessingException extends RuntimeException {
    /**
     * {@link Element} that has errors when processing.<br/>
     * This is used to notify users about where the processing error occurred.
     */
    private Element invalidElement;

    public ProcessingException() {
    }

    public ProcessingException(String s) {
        super(s);
    }

    public ProcessingException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ProcessingException(Throwable throwable) {
        super(throwable);
    }

    public Element getInvalidElement() {
        return invalidElement;
    }

    public ProcessingException setInvalidElement(Element invalidElement) {
        this.invalidElement = invalidElement;
        return this;
    }
}
