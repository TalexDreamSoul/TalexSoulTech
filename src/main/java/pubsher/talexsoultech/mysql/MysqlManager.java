package pubsher.talexsoultech.mysql;

import pubsher.talexsoultech.builder.*;
import pubsher.talexsoultech.utils.LogUtil;

import java.sql.*;

public class MysqlManager extends LogUtil {

    public static MysqlManager instance = null;
    private Connection connection;

    public static MysqlManager get() {

        return instance == null ? instance = new MysqlManager() : instance;
    }

    public boolean connectMySQL(String ip, int port, String databaseName, String userName, String password, boolean useSSL) {

        databaseName = databaseName.toLowerCase();

        try {

            this.connection = DriverManager.getConnection("jdbc:mysql://" + ip + ":" + port + "/" + databaseName + "?autoReconnect=true&serverTimezone=Asia/Shanghai&useSSL=" + useSSL, userName, password);

        } catch ( SQLException e ) {

            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return false;

        }

        return true;

    }

    public boolean autoAccess(SqlBuilder sb) {

        PreparedStatement ps;

        try {

            ps = this.connection.prepareStatement(sb.toString());
            return ps.execute();

        } catch ( SQLException throwables ) {

            LogUtil.log("[数据库] [自动执行] 发生异常: " + throwables.getMessage() + " # 命令: " + sb);
            return false;

        }

    }

    public ResultSet likeData(SqlLikeBuilder slb) {

        PreparedStatement ps;

        try {

            ps = this.connection.prepareStatement(slb.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            return ps.executeQuery();

        } catch ( SQLException throwables ) {

            LogUtil.log("[数据库] [搜索数据] 发生异常: " + throwables.getMessage() + " # " + slb);
            return null;

        }

    }

    public boolean updateData(SqlUpdBuilder upd) {

        PreparedStatement ps;

        try {

            ps = this.connection.prepareStatement(upd.toString());
            return ps.execute();

        } catch ( SQLException throwables ) {

            LogUtil.log("[数据库] [更新数据] 发生异常: " + throwables.getMessage());
            return false;

        }

    }

    public boolean addData(SqlAddBuilder sab) {

        PreparedStatement ps;

        try {

            ps = this.connection.prepareStatement(sab.toString());
            return ps.execute();

        } catch ( SQLException throwables ) {

            LogUtil.log("[数据库] [添加数据] 发生异常: " + throwables.getMessage() + " # 命令: " + sab);
            return false;

        }

    }

    public boolean deleteData(String table, String type, String value) {

//        DELETE FROM `users` WHERE `name` = \"" + user.getUsername() + "\"

        try {

            PreparedStatement ps = this.connection.prepareStatement(SQLCommand.DELETE_DATA.commandToString()
                    .replaceFirst("%table_name%", table.replace("--", ""))
                    .replaceFirst("%username%", type.replace("--", ""))
                    .replaceFirst("%value%", value));

            return ps.execute();

        } catch ( SQLException throwables ) {

            LogUtil.log("[数据库] [删除数据] 发生异常: " + throwables.getMessage());
            return false;

        }


    }

    public void joinTable(SqlTableBuilder stb) {

        String cmd = stb.toString();

        //System.out.println(cmd);

        try {

            PreparedStatement ps = connection.prepareStatement(cmd);

            ps.executeUpdate();

        } catch ( SQLException e ) {

            LogUtil.log("[数据库] 在创建数据表的时候发生了异常,执行命令: " + cmd);
            LogUtil.log("[数据库] 错误信息: " + e.getMessage());

            e.printStackTrace();

        }

//        LogUtil.printInfo("[MYSQL] 数据库 " + ansi().fgBrightCyan().a(stb.getTableName()).fgGreen() + " 已创建,参数: " + stb.getMap().size());

    }

    public ResultSet readSearchAllData(String table) {

        try {

            PreparedStatement ps = this.connection.prepareStatement(SQLCommand.SELECT_ALL_DATA.commandToString()
                    .replaceFirst("%table_name%", table), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ps.setFetchSize(Integer.MIN_VALUE);

            return ps.executeQuery();

        } catch ( SQLException e ) {

            LogUtil.log("FindAllData # " + e.getMessage());
            return null;

        }

    }

    public ResultSet readSearchData(String table, String selectType, String value) {

        String s = "NONE";

        try {

            PreparedStatement ps = this.connection.prepareStatement(SQLCommand.SELECT_DATA.commandToString()
                    .replaceFirst("%select_type%", selectType)
                    .replaceFirst("%table_name%", table)
                    .replaceFirst("%username%", value), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ps.setFetchSize(Integer.MIN_VALUE);

            return ps.executeQuery();

        } catch ( SQLException e ) {

            // TODO 自动生成的 catch 块
            LogUtil.log(e.getMessage() + " # Execute: " + s);
            return null;

        }

    }

    public void shutdown() {

        if ( connection == null ) {
            return;
        }

        LogUtil.log("[数据库] 数据库已停止服务!");

        try {
            connection.close();
        } catch ( SQLException e ) {
            //断开连接失败
            e.printStackTrace();
        }

    }

    public boolean isServiceNull() {

        return this.connection == null;

    }

    public boolean prepareStatement(String sql) {

        try {

            return this.connection.prepareStatement(sql).execute();

        } catch ( SQLException throwables ) {

            LogUtil.log("[数据库] [预备] 在执行指令的时候发生了错误: " + sql);
            return false;

        }

    }

}
