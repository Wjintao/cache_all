1、将easymem.properties放到项目的config的根目录，easymem.jar包放入工程的依赖classpath中

2、在easymem.properties的配置实际对应Memcache的IP地址和端口，支持多个负载均衡，以逗号分割
#server列表
server.list=localhost:11211,10.3.3.12:1122

3、调用easymem.jar提供的公共方法对Memcache进行读取，客户端只需要关心两个public类
com.asiainfo.easymem.DefaultEasyMemClient
com.asiainfo.easymem.EasyMemUtil

统一接口侧调用easymem公用方法进行读取，读取例子方法如下：
3.1放置数据到Memcache
public class TestPut {
	public static void main(String[] args) throws Exception{
		if(EasyMemUtil.isEnable()){
			DefaultEasyMemClient objDefaultEasyMemClient = DefaultEasyMemClient.getInstance();
			// 放置字符串
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName1", "江西1");
			objDefaultEasyMemClient.setKeyAndValue2AllServer("testName2", "江西2");
			// 放置对象
			Map map = new HashMap();
			map.put("age", "17岁");
       		objDefaultEasyMemClient.setKeyAndValue2AllServer("testAgeMap", map);
		}
	}
}


3.2从Memcache获取数据
public class TestGet { 
	public static void main(String[] args) throws Exception{
		if(EasyMemUtil.isEnable()){
			// 一个个获取
			System.out.println("key=testName1的值为: " + DefaultEasyMemClient.getInstance().get("testName1"));
			System.out.println("key=testName1的值为: " + DefaultEasyMemClient.getInstance().get("testName2"));
			// 获取对象
			System.out.println("key=testAgeMap的值为: " + DefaultEasyMemClient.getInstance().get("testAgeMap"));
			// 批量获取
			Map map1 = DefaultEasyMemClient.getInstance().getMultiArray(new String[] { "testName1", "testName2" });
			System.out.println("key=testName1和testName2的值为" + map1);
		}
	}
}
