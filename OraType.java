/**
 * export ORACLE_HOME=/path/to/oracle/home
 * export LD_LIBRARY_PATH=$ORACLE_HOME/lib:$LD_LIBRARY_PATH
 *
 * Compile:
 * javac -classpath $ORACLE_HOME/jdbc/lib/ojdbc6.jar OraType.java

 * Run:
 *  java -classpath .:${ORACLE_HOME}/jdbc/lib/ojdbc6.jar \
 *    OraType "user/pass@host:1521:sid" 1 One 2017-0l-04

 * java -classpath .:$ORACLE_HOME/jdbc/lib/ojdbc6.jar \
 *    OraType "user/pass@host:1521:sid" 1
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

class ora_type implements ORAData, ORADataFactory
{
  static final ora_type _ora_typeFactory = new ora_type();

  public int n;
  public String v;
  public Date d;

  public static ORADataFactory getORADataFactory() {
    return _ora_typeFactory;
  }

  public ora_type () {}
  public ora_type(int n, String v, Date d)
  {
    this.n = n;
    this.v = v;
    this.d = d;
  }

  @Override
  public Datum toDatum(Connection conn) throws SQLException
  {
    StructDescriptor sd = StructDescriptor.createDescriptor("ORA_TYPE", conn);
    Object [] attributes = {
      n,
      v,
      d
    };
    return new STRUCT(sd, conn, attributes);
  }

  public ORAData create(Datum datum, int sqlType) throws SQLException
  {
    if (datum == null) return null;

    Object [] attributes = ((STRUCT) datum).getOracleAttributes();

    return new ora_type(
      (int) attributes[0],
      (String) attributes[1],
      (Date) attributes[2]
    );
  }
}

public class OraType {

    private Connection conn;

    private void connect(String url) throws SQLException {
        OracleDataSource ds = new OracleDataSource();
        ds.setURL("jdbc:oracle:thin:" + url);

        conn = ds.getConnection();
        conn.setAutoCommit(false);
    }

    public static void main(String[] args) {

      //System.out.println("args.length: " + args.length);
      //System.out.println("args[0]: " + args[0]);
      OraType c;

      try {
        switch (args.length) {
          case 4:
                c = new OraType();
                c.connect(args[0]);
                c.set(Integer.parseInt(args[1]), args[2], args[3]);
                break;
          case 2:
                c = new OraType();
                c.connect(args[0]);
                c.get(Integer.parseInt(args[1]));
                break;
          default:
                System.out.println("Wrong parameters");
        }
      } catch (Exception e) {
        //System.out.println("Exception: " + e.getMessage());
        e.printStackTrace();
      }
    }

    void set(int n, String v, String d) throws SQLException, java.text.ParseException {
      OracleCallableStatement stmt;

			// Prepare Java object
			ora_type oType = new ora_type();
			oType.n = n;
			oType.v = v;
			oType.d =
        new java.sql.Date((new SimpleDateFormat("yyyy-MM-dd")).parse(d).getTime());

      stmt = (OracleCallableStatement)conn.prepareCall("{?= call ora_func_set(?)}");

      stmt.registerOutParameter(1, OracleTypes.INTEGER);
      stmt.setORAData(2, oType); // or we can use stmt.setObject(2, oType);
      stmt.execute();
      conn.commit();

      System.out.println("Return value: " + stmt.getInt(1));
    }

    void get(int p) throws SQLException {
      CallableStatement stmt;

      stmt = conn.prepareCall("{?= call ora_func_get(?)}");

      stmt.registerOutParameter(1, OracleTypes.STRUCT, "ORA_TYPE");
      stmt.setInt(2, p);
      stmt.execute();

      java.sql.Struct jdbcStruct = (java.sql.Struct)stmt.getObject(1);
      Object[] o = jdbcStruct.getAttributes();
      int n = ((java.math.BigDecimal)o[0]).intValueExact();
      String v = (String)o[1];
      java.sql.Timestamp d = (java.sql.Timestamp)o[2];

      System.out.println("n: " + n);
      System.out.println("v: " + v);
      System.out.println("d: " + d);
    }
}
