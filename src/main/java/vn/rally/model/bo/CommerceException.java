package vn.rally.model.bo;

public final class CommerceException extends Exception {
    private final boolean conflict;

    CommerceException(String message) {
        this(message, false);
    }

    CommerceException(String message, boolean conflict) {
        super(message);
        this.conflict = conflict;
    }

    public boolean isConflict() {
        return conflict;
    }
}
