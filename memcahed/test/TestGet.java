import java.util.Map;

import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.EasyMemUtil;



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
