/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.repository;
import cn.zhuatech.helpdesk.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SystemSettingRepository extends JpaRepository<SystemSetting,String>{}
