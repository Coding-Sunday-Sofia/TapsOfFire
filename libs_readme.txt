It turned out that Android doesn't like .svn folder in 'libs' - it generates
currupted apk file in this case. So I've removed libs from the svn, and you
need to rename 'libs_' to 'libs' before running.