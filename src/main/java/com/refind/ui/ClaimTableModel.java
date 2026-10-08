package com.refind.ui;

import com.refind.model.Claim;
import com.refind.model.enums.ClaimStatus;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClaimTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Item Title", "Item Type", "Claimant", "Message", "Status", "Submitted At"
    };

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private List<Claim> claims = new ArrayList<>();

    public void setClaims(List<Claim> claims) {
        this.claims = claims != null ? new ArrayList<>(claims) : new ArrayList<>();
        fireTableDataChanged();
    }

    public Claim getClaimAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < claims.size()) {
            return claims.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return claims.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Claim c = claims.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> "#" + c.getId();
            case 1 -> c.getItem() != null ? c.getItem().getTitle() : "Item #" + (c.getItem() != null ? c.getItem().getId() : "?");
            case 2 -> c.getItem() != null && c.getItem().getType() != null ? c.getItem().getType().name() : "N/A";
            case 3 -> c.getClaimant() != null ? (c.getClaimant().getName() != null ? c.getClaimant().getName() : "User #" + c.getClaimant().getId()) : "Unknown";
            case 4 -> c.getMessage() != null && !c.getMessage().isBlank() ? c.getMessage() : "(No message)";
            case 5 -> c.getStatus() != null ? c.getStatus().name() : ClaimStatus.PENDING.name();
            case 6 -> c.getSubmittedAt() != null ? c.getSubmittedAt().format(FORMATTER) : "N/A";
            default -> "";
        };
    }
}
