#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/inotify.h>
#include <unistd.h>

/* 内全局变量begin */
static jboolean IS_COPY = JNI_TRUE;

void Java_cn_hiroz_uninstallfeedback_FeedbackUtils_init(JNIEnv* env, jobject thiz, jstring dirStr, jstring activity, jstring action, jstring data) {
	if (0 == fork()) {
		int fileDescriptor = inotify_init();
		if (fileDescriptor < 0) {
			//inotify_init failed
			exit(1);
		}

		int watchDescriptor;
		watchDescriptor = inotify_add_watch(fileDescriptor, (*env)->GetStringUTFChars(env, dirStr, &IS_COPY), IN_DELETE);
		if (watchDescriptor < 0) {
			//inotify_add_watch failed
			exit(1);
		}

		void *p_buf = malloc(sizeof(struct inotify_event));
		if (p_buf == NULL) {
			exit(1);
		}
		size_t readBytes = read(fileDescriptor, p_buf, sizeof(struct inotify_event));
		free(p_buf);
		inotify_rm_watch(fileDescriptor, IN_DELETE);

		//4.2以上的系统需要加上 --user 0
		if (activity == NULL || (*env)->GetStringUTFLength(env, activity) < 1) {
			execlp("am", "am", "start", "--user", "0", "-a", (*env)->GetStringUTFChars(env, action, &IS_COPY), "-d", (*env)->GetStringUTFChars(env, data, &IS_COPY), (char *) NULL);
		} else {
			if (action == NULL || (*env)->GetStringUTFLength(env, action) < 1) {
				if (data == NULL || (*env)->GetStringUTFLength(env, data) < 1) {
					execlp("am", "am", "start", "--user", "0", "-n", (*env)->GetStringUTFChars(env, activity, &IS_COPY), (char *) NULL);
				} else {
					execlp("am", "am", "start", "--user", "0", "-n", (*env)->GetStringUTFChars(env, activity, &IS_COPY), "-d", (*env)->GetStringUTFChars(env, data, &IS_COPY), (char *) NULL);
				}
			} else {
				if (data == NULL || (*env)->GetStringUTFLength(env, data) < 1) {
					execlp("am", "am", "start", "--user", "0", "-n", (*env)->GetStringUTFChars(env, activity, &IS_COPY), "-a", (*env)->GetStringUTFChars(env, action, &IS_COPY), (char *) NULL);
				} else {
					execlp("am", "am", "start", "--user", "0", "-n", (*env)->GetStringUTFChars(env, activity, &IS_COPY), "-a", (*env)->GetStringUTFChars(env, action, &IS_COPY), "-d", (*env)->GetStringUTFChars(env, data, &IS_COPY), (char *) NULL);
				}
			}
		}
	}
}