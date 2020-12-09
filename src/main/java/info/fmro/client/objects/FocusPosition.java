package info.fmro.client.objects;

import org.jetbrains.annotations.Nullable;

// this class is supposed to be used in the GUI, so it's not synchronized
public class FocusPosition {
    private int columnIndex = -1, rowIndex = -1, caretPosition = -1;
    private @Nullable String id; // the marketId or eventId

    public void setFocus(@Nullable final String id, final int columnIndex, final int rowIndex, final int caretPosition) {
        setId(id);
        setColumnIndex(columnIndex);
        setRowIndex(rowIndex);
        setCaretPosition(caretPosition);
    }

    public int getColumnIndex() {
        return this.columnIndex;
    }

    private void setColumnIndex(final int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public int getRowIndex() {
        return this.rowIndex;
    }

    private void setRowIndex(final int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getCaretPosition() {
        return this.caretPosition;
    }

    private void setCaretPosition(final int caretPosition) {
        this.caretPosition = caretPosition;
    }

    public @Nullable String getId() {
        return this.id;
    }

    private void setId(@Nullable final String id) {
        this.id = id;
    }

    private boolean idEquals(final String checkedId) {
        return checkedId != null && checkedId.equals(this.id);
    }

    private boolean columnEquals(final int checkedColumn) {
        return checkedColumn >= 0 && checkedColumn == this.columnIndex;
    }

    private boolean rowEquals(final int checkedRow) {
        return checkedRow >= 0 && checkedRow == this.rowIndex;
    }

    public boolean positionEquals(final String checkedId, final int checkedColumn, final int checkedRow) {
        return idEquals(checkedId) && columnEquals(checkedColumn) && rowEquals(checkedRow);
    }

    public boolean reset(final String checkedId, final int checkedColumn, final int checkedRow) {
        final boolean hasReset;
        if (positionEquals(checkedId, checkedColumn, checkedRow)) {
            reset();
            hasReset = true;
        } else { // checked element is not focused, nothing to be done
            hasReset = false;
        }
        return hasReset;
    }

    private void reset() {
        this.id = null;
        this.columnIndex = -1;
        this.rowIndex = -1;
        this.caretPosition = -1;
    }
}
