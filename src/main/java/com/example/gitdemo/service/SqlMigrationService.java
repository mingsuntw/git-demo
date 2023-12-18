package com.example.gitdemo.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Service
public class SqlMigrationService {

    public static final String REPO_PATH = "/Users/mingsun/IdeaProjects/OTR/sql-script";
    public static final String PERSONAL_ACCESS_TOKEN = "personal-access-token";

    public String processSqlScript(String jiraCard,
                                   String username,
                                   String description,
                                   String sqlScript) {
        try (Repository repository = Git.open(new File(REPO_PATH))
            .getRepository()) {
            Git git = new Git(repository);

            // 使用JGit创建分支
            String branchName = checkoutBranch(git, "master", jiraCard);

            // 在指定位置创建新文件并写入SQL脚本
            createAndWriteToFile(description, sqlScript, "migration");

            commitChanges(git, description);

            // 使用JGit推送代码到远程仓库
            pushChanges(git, branchName);

            // 创建PR并获取PR链接
            String prLink = createPullRequest(branchName);

            // list pull requests url https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#list-pull-requests

            return prLink;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String checkoutBranch(Git git, String baseBranch, String jiraCard)
        throws GitAPIException {
        String branchName = "sql_" + jiraCard;

        Iterable<Ref> refs = git.branchList().call();

        boolean branchExists = false;
        for (Ref ref : refs) {
            if (ref.getName().equals("refs/heads/" + branchName)) {
                branchExists = true;
                break;
            }
        }
        git.checkout()
            .setCreateBranch(!branchExists)
            .setName(branchName)
            .setStartPoint(baseBranch)
            .call();
        return branchName;
    }

    private void createAndWriteToFile(String description, String sqlScript, String path) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = String.format("V%s__%s.sql", timestamp, description.replace(" ", "_"));

        String filePath = String.format("%s/src/main/resources/%s/%s", REPO_PATH, path, fileName);
        // 写入内容到迁移脚本文件
        try {
            File file = new File(filePath);
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(sqlScript);
            fileWriter.close();

        } catch (IOException e) {
            System.out.println(
                "An error occurred while writing to the migration file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void commitChanges(Git git, String description) throws GitAPIException {
        // 将新文件添加到暂存区
        git.add().addFilepattern(".").call();

        // 提交文件
        git.commit().setMessage("Adding SQL script " + description).call();
    }

    private void pushChanges(Git git, String branchName)
        throws GitAPIException, URISyntaxException {

        // 设置远程仓库 URL 和凭据
        String remoteRepoURI = "https://github.com/mingsuntw/sql-script.git"; // 远程仓库 URL

        CredentialsProvider credentialsProvider =
            new UsernamePasswordCredentialsProvider(PERSONAL_ACCESS_TOKEN, "");

        // 添加远程仓库并设置凭据
        git.remoteAdd().setName("origin").setUri(new URIish(remoteRepoURI)).call();

        // 创建并设置RefSpec
        RefSpec refSpec = new RefSpec("refs/heads/" + branchName + ":refs/heads/" + branchName);

        // 推送指定分支到远程仓库
        git.push()
            .setCredentialsProvider(credentialsProvider)
            .setRemote("origin")
            .setRefSpecs(refSpec)
            .call();
    }

    // JGit库本身并没有直接支持创建Pull Request（PR）的功能，它主要用于Git版本控制的操作，例如提交、拉取、推送等。要创建PR，你需要通过Git服务器的API进行操作，而不是直接使用JGit库。
    //如果你想在Java中创建PR，可以使用GitHub API (https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#create-a-pull-request) 或GitLab API等服务提供的RESTful接口。
    private String createPullRequest(String branchName) {
        String owner = "mingsuntw"; // 替换为你的仓库所有者
        String repo = "sql-script"; // 替换为你的仓库名称
        String baseBranch = "master"; // 目标分支
        String headBranch = branchName; // 源分支
        String title = "Pull Request Title" + branchName;
        String body = "Pull Request Description";
        String token = PERSONAL_ACCESS_TOKEN; // 替换为你的GitHub访问令牌

        try {
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            String json =
                "{\"title\": \"" + title + "\", \"body\": \"" + body + "\", \"head\": \"" +
                    headBranch + "\", \"base\": \"" + baseBranch + "\"}";
            RequestBody bodyRequest = RequestBody.create(json, mediaType);

            Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + owner + "/" + repo + "/pulls")
                .post(bodyRequest)
                .addHeader("Authorization", "token " + token)
                .addHeader("Content-Type", "application/json")
                .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("Pull Request created successfully.");
                    System.out.println("PR URL: " + response.header("Location"));
                } else {
                    System.out.println(
                        "Failed to create Pull Request. Response code: " + response.code());
                }

                // 获取响应中的Location头部信息，该头部包含了PR的URL
                String prURL = response.header("Location");

                return prURL;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 处理异常
        }
        return null;
    }

}
