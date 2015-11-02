#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/inotify.h>
#include <unistd.h>
#include <time.h>

/* 内全局变量begin */
static jboolean IS_COPY = JNI_TRUE;

void Java_cn_hiroz_uninstallfeedback_FeedbackUtils_init(JNIEnv* env, jobject thiz, jint is_fork, jstring dirStr, jstring activity, jstring action, jstring data, jstring brand) {
	if (is_fork == 0 || 0 == fork()) {
		const char* dir_chars = (*env)->GetStringUTFChars(env, dirStr, &IS_COPY);

		const char* brand_chars = (*env)->GetStringUTFChars(env, brand, &IS_COPY);

		const char* activity_chars;
		int activity_length = 0;
		if (activity != NULL) {
			activity_length = (*env)->GetStringUTFLength(env, activity);
			activity_chars = (*env)->GetStringUTFChars(env, activity, &IS_COPY);
		}

		const char* action_chars;
		int action_length = 0;
		if (action != NULL) {
			action_length = (*env)->GetStringUTFLength(env, action);
			action_chars = (*env)->GetStringUTFChars(env, action, &IS_COPY);
		}

		const char* data_chars;
		int data_length = 0;
		if (data != NULL) {
			data_length = (*env)->GetStringUTFLength(env, data);
			data_chars = (*env)->GetStringUTFChars(env, data, &IS_COPY);
		}

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
				break;
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

		__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "1");

		execlp("sh", "echo", "started");

		__android_log_write(ANDROID_LOG_DEBUG, "DaemonThread", "2");

		//4.2以上的系统需要加上 --user 0
		if (activity == NULL || activity_length < 1) {
			execlp("am", "am", "start", "--user", "0", "-a", action_chars, "-d", data_chars, (char *) NULL);
		} else {
			if (action == NULL || action_length < 1) {
				if (data == NULL || data_length < 1) {
					execlp("am", "am", "start", "--user", "0", "-n", activity_chars, (char *) NULL);
				} else {
					execlp("am", "am", "start", "--user", "0", "-n", activity_chars, "-d", action_chars, (char *) NULL);
				}
			} else {
				if (data == NULL || data_length < 1) {
					execlp("am", "am", "start", "--user", "0", "-n", activity_chars, "-a", action_chars, (char *) NULL);
				} else {
//					execlp("am", "am", "start","--user", "0" ,"-a", "android.intent.action.VIEW", "-d", "http://www.360.cn", (char *)NULL);
					execlp("am", "am", "start", "--user", "0", "-n", activity_chars, "-a", action_chars, "-d", data_chars, (char *) NULL);
				}
			}
		}
	}
}