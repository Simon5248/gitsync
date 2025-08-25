package com.example.gitsync.factory;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;

/**
 * 自訂 SSH Session Factory，用於 JGit。
 * 這個實作會：
 * 1. 停用嚴格的主機金鑰檢查 (StrictHostKeyChecking=no)。
 * 2. 使用指定的私鑰檔案進行身分驗證。
 */
public class CustomSshSessionFactory extends JschConfigSessionFactory {

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        // 設定不進行嚴格的主機金鑰檢查，避免第一次連線時需要手動確認
        session.setConfig("StrictHostKeyChecking", "no");
    }

}

