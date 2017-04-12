/**
 * Based on "Oracle Database JDBC Developer’s Guide 11g Release 2 (11.2)"
 *
 * Oracle's ojdbc6.jar is needed to run this example
 *
 * Compile:
 * javac -classpath .:/path/to/jdbc/lib/ojdbc6.jar OraType.java
 *
 * Run:
 *  java -classpath .:/path/to/jdbc/lib/ojdbc6.jar \
 *    OraType "user/pass@host:1521:sid" 1 One 2017-0l-04
 *
 * java  -classpath .:/path/to/jdbc/lib/ojdbc6.jar \
 *     OraType "user/pass@host:1521:sid" 1
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.CallableStatement;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleTypes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import oracle.sql.StructDescriptor;
import oracle.sql.STRUCT;
import java.util.Date;

import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.jdbc.OracleCallableStatement;

// The type map (between Oracle type 'create type as ...' and Java class)
// is not required when using Java classes that implement ORAData.
class ora_type implements ORAData, ORADataFactory
{
  static final ora_type _ora_typeFactory = new ora_type();

  // The same fields as in Oracle type (ORA_TYPE)
  public int n;
  public String v;
  public Date d;

  // Constructors
  public ora_type () {}
  public ora_type(int n, String v, Date d)
  {
    this.n = n;
    this.v = v;
    this.d = d;
  }

  // Implement a method that produces the ORADataFactory instance
  // for use with getORAData().
  public static ORADataFactory getORADataFactory() {
    return _ora_typeFactory;
  }

  // Implement interfaces for converting types between Oracle and Java
  @Override
  public Datum toDatum(Connection conn) throws SQLException
  {
    StructDescriptor sd = StructDescriptor.createDescriptor("ORA_TYPE", conn);
    Object [] attributes = {n, v, d };
    return new STRUCT(sd, conn, attributes);
  }

  // The JDBC driver will call create() from the object passed to getORAData(),
  // returning to your Java application an instance of this class (ora_type)
  //
  // Currently parameter sqlType is not used,
  // but can be used to handle type and subtypes
  @Override
  public ORAData create(Datum datum, int sqlType) throws SQLException
  {
    if (datum == null) return null;

    Object [] attributes = ((STRUCT) datum).getOracleAttributes();

    // Map and convert Oracle types (which are fields in ORA_TYPE)
    // to Java class fields
    return new ora_type(
      ((oracle.sql.NUMBER) attributes[0]).intValue(),
      ((oracle.sql.CHAR)   attributes[1]).stringValue(),
      ((oracle.sql.DATE)   attributes[2]).dateValue()
    );
  }
}

public class OraType {

    private Connection conn;

    private void connect(String url) throws SQLException
    {
        OracleDataSource ds = new OracleDataSource();
        ds.setURL("jdbc:oracle:thin:" + url);

        conn = ds.getConnection();
        conn.setAutoCommit(false);
    }
    private void commit() throws SQLException
    {
      if (conn != null) { conn.commit(); }
      System.out.println("COMMIT.");
    }

    private void rollback()
    {
      try {
        if (conn != null) { conn.rollback(); }
        System.out.println("ROLLBACK.");
      } catch (SQLException e) {
        System.out.println("Rollback: " + e.getMessage());
      }
    }

    public static void main(String[] args)
    {
      // Input parameters
      int    n;
      String v;
      Date   d;

      // Class mapped to Oracle type
      ora_type oraType;

      OraType c = new OraType();
      try {
        switch (args.length) {
          case 4:
                c.connect(args[0]);

                // Parse cmdline parameters
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                n = Integer.parseInt(args[1]);
                v = args[2];
                d = new java.sql.Date(dateFormat.parse(args[3]).getTime());

                // Pass object to database
                oraType = new ora_type(n, v, d);
                int ret = c.set(oraType);
                System.out.println("Return value: " + ret);
                c.commit();

                break;
          case 2: // Receive object from database
                c.connect(args[0]);
                n = Integer.parseInt(args[1]);

                oraType = c.get(n);

                System.out.println("n: " + oraType.n);
                System.out.println("v: " + oraType.v);
                System.out.println("d: " + oraType.d);
                break;
          default:
                System.out.println("Wrong parameters");
        }
      } catch (Exception e) {
        System.out.println("Exception: " + e.getMessage());
        //e.printStackTrace();
        c.rollback();
      }
    }

    int set(ora_type p) throws SQLException, java.text.ParseException
    {

      OracleCallableStatement stmt =
        (OracleCallableStatement)conn.prepareCall("{?= call ora_func_set(?)}");

      stmt.registerOutParameter(1, OracleTypes.INTEGER);
      stmt.setORAData(2, p);
      stmt.execute();
      return stmt.getInt(1);
    }

    ora_type get(int p) throws SQLException
    {
      OracleCallableStatement stmt =
        (OracleCallableStatement)conn.prepareCall("{?= call ora_func_get(?)}");

      stmt.registerOutParameter(1, OracleTypes.STRUCT, "ORA_TYPE");
      stmt.setInt(2, p);
      stmt.execute();

      return (ora_type)stmt.getORAData(1, ora_type.getORADataFactory());
    }
}
