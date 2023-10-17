import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseUpdater {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/sqltool";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "shangyi";

    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for(int i=0;i<5;i++){
            final int recordId = 1;
            final String newValue = "New value " + (i+1);
            executorService.submit(()->{
                updateDatabaseField(recordId,newValue);
            });
        }
//        updateDatabaseField(1, "New Value");
    }

    private static void updateDatabaseField(int recordId, String newValue) {
        Connection connection = null;
        try {
            // Establish database connection
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            connection.setAutoCommit(false);  // Start a transaction

            // Check the current version of the record
            int currentVersion = getCurrentVersion(connection, recordId);

            // Perform the update using optimistic locking
            updateRecord(connection, recordId, newValue, currentVersion);

            // Commit the transaction
            connection.commit();
        } catch (SQLException e) {
            // Handle exceptions, possibly rollback the transaction
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            // Close the connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }

    private static int getCurrentVersion(Connection connection, int recordId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT version FROM test WHERE id = ?")) {
            preparedStatement.setInt(1, recordId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("version");
                }
            }
        }
        return -1;  // Return a default value or handle appropriately
    }

    private static void updateRecord(Connection connection, int recordId, String newValue, int currentVersion) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE test SET value = ?, version = version + 1,age = age+1 WHERE id = ? AND version = ?")) {
            preparedStatement.setString(1, newValue);
            preparedStatement.setInt(2, recordId);
            preparedStatement.setInt(3, currentVersion);
            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated == 0) {
                // No rows were updated, indicating a potential conflict
                throw new SQLException("Optimistic locking failure");
            }
        }
    }
}

