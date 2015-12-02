#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/inotify.h>
#include <unistd.h>
#include <time.h>
#include <dirent.h>

/* 内全局变量begin */
static jboolean IS_COPY = JNI_TRUE;

jint Java_cn_hiroz_uninstallfeedback_FeedbackUtils_init(JNIEnv* env, jobject thiz, jint is_fork, jstring dirStr, jstring data, jstring brand, jint api_level, jstring process_name) {
	int forkProcess = 0;
	if (is_fork == 0 || 0 == (forkProcess = fork())) {

		if (is_fork == 1) {

			int success = 1;
			jint sdkInt = 0;
			jclass class_id = (*env)->FindClass(env, "cn/hiroz/uninstallfeedback/AppProcessEntry");
			if (class_id == NULL) {
				success = 0;
			}

			jmethodID method_id;
			if (success == 1) {
				method_id = (*env)->GetStaticMethodID(env, class_id, "setProcessName", "(Ljava/lang/String;)V");
				if (method_id == NULL) {
					success = 0;
				}
			}

			if (success == 1) {
				(*env)->CallStaticVoidMethod(env, class_id, method_id, process_name);
			}

		}

		const char* dir_chars = (*env)->GetStringUTFChars(env, dirStr, &IS_COPY);

		const char* brand_chars = (*env)->GetStringUTFChars(env, brand, &IS_COPY);

//		const char* data_chars;
//		int data_length = 0;
//		if (data != NULL) {
//			data_length = (*env)->GetStringUTFLength(env, data);
//			data_chars = (*env)->GetStringUTFChars(env, data, &IS_COPY);
//		}

		const char* data_chars = (*env)->GetStringUTFChars(env, data, &IS_COPY);

//		char data_chars[512];
//		sprintf(data_chars, "%s", (*env)->GetStringUTFChars(env, data, &IS_COPY));

		while (1) {

			int fileDescriptor = inotify_init();
			if (fileDescriptor < 0) {
				//inotify_init failed
				exit(1);
			}

			int watchDescriptor;
			watchDescriptor = inotify_add_watch(fileDescriptor, dir_chars, IN_DELETE);
			if (watchDescriptor < 0) {
				//inotify_add_watch failed
				exit(1);
			}

			void *p_buf = malloc(sizeof(struct inotify_event));
			if (p_buf == NULL) {
				exit(1);
			}

			__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "Start Observer");

			size_t readBytes = read(fileDescriptor, p_buf, sizeof(struct inotify_event));

			__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "Delete");

			free(p_buf);

			inotify_rm_watch(fileDescriptor, IN_DELETE);

			// 方案一：卸载后等待两秒
//			sleep(2)

			// 方案二：卸载后等待两秒
//			long start_time = time((time_t*) NULL);
//			long current_time;
//
//			while(1) {
//				current_time = time((time_t*) NULL);
//				__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "current_time => %d", current_time);
//				if (current_time - start_time >= 1000) {
//					break;
//				}
//			}

			if (strcmp(brand_chars, "Huawei") == 0) {
				// 做一个延时操作
				int i = 0;
				for (i = 0; i < 300; i++) {
					__android_log_write(ANDROID_LOG_DEBUG, "init", "tic-toc");
				}
			} else {
				sleep(1);
			}

			// 两秒后再判断目录是否存在，如果还存在，有可能是覆盖安装
			if (!access(dir_chars, 0)) {
				// 覆盖安装什么都不用做，重新监听目录删除
				__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "replace install");
			} else {
				// 不是覆盖安装，应该是真被删除了
				__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "will be startup intent");
				break;
			}

		}

//		execlp("am", "am", "start", "-a", "android.intent.action.VIEW", "-d", "http://www.360.cn", (char *)NULL);
//		execlp("am", "am", "start","--user", "0" ,"-a", "android.intent.action.VIEW", "-d", "http://www.360.cn", (char *)NULL);

//		char ver[20];
//		sprintf(ver, "version => %d", api_level);
//		__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", ver);

		//4.1以上的系统需要加上 --user 0
		//区分Android 4.x之前的版本 和 Android 4.x之后的版本的API变化
		if (api_level > 16) {
			execlp("am", "am", "start", "--user", "0", "-a", "android.intent.action.VIEW", "-d", data_chars, (char *) NULL);
		} else {
			execlp("am", "am", "start", "-a", "android.intent.action.VIEW", "-d", data_chars, (char *) NULL);
		}

	}
	return forkProcess;
}

//jint Java_cn_hiroz_uninstallfeedback_FeedbackUtils_countProcess(JNIEnv* env, jobject thiz, jstring process_name) {
//	char c_process_name[255];
//	sprintf(c_process_name, "%s", (*env)->GetStringUTFChars(env, process_name, &IS_COPY));
//	DIR *d;
//	int threadcount;
//	threadcount = 0;
//	struct dirent *de;
//	char *namefilter = 0;
//	int threads = 0;
//	d = opendir("/proc");
//	if (d != 0) {
//		while ((de = readdir(d)) != 0) { //遍历目录
//			if (de->d_type == DT_DIR) {
//				if (isdigit(de->d_name[0])) {
//					//pid拿到了之后，从pid里边读取其他的信息，主要是要得到进程的名称（NAME）属性
//					char chrarry_CommandLinePath[512];
//					char chrarry_NameOfProcess[300];
//					//这里可能存在溢出的安全隐患
//					sprintf(chrarry_CommandLinePath, "/proc/%s/cmdline", de->d_name);
//					FILE* fd_CmdLineFile = fopen(chrarry_CommandLinePath, "rt");
//					if (fd_CmdLineFile) {
//						fscanf(fd_CmdLineFile, "%s", chrarry_NameOfProcess);
//						fclose(fd_CmdLineFile);
//						//进程的名称（NAME）到手之后，比较一下是不是我们自己的包的名字，是的话，就计数器+1
//						if (strstr(chrarry_NameOfProcess, c_process_name) != NULL) {
//							threadcount++;
//						}
//					}
//				}
//			}
//		}
//		closedir(d);
//	}
//	return threadcount;
//}