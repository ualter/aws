** Commands AWS CLI - Utilities
aws iam get-role --role-name role-tasadora

aws iam get-user --user-name ualter



** Update AWS SDK 1.1 with My Modifications 
jar -tvf /Users/ualter/.m2/repository/com/amazonaws/aws-java-sdk-core/1.11.454/aws-java-sdk-core-1.11.454.jar | grep AWS4Signer.class

jar uf /Users/ualter/.m2/repository/com/amazonaws/aws-java-sdk-core/1.11.454/aws-java-sdk-core-1.11.454.jar com/amazonaws/auth/AWS4Signer.class

find . -name "*AWS4Signer.class" | xargs ls -l


** Apache Reverse Proxy Commands
sudo /usr/sbin/apachectl status
sudo /usr/sbin/apachectl start
sudo /usr/sbin/apachectl stop 
sudo /usr/sbin/apachectl restart

** Apache MacOS Paths Config
/private/etc/apache2/httpd.conf

** Apache MacOS Logs
/private/var/log/apache2/access_log
/private/var/log/apache2/error_log
/private/var/log/apache2/forensic_log

** Virtual Hosts Configured
/private/etc/apache2/extra/httpd-vhosts.conf

** My Conf with Hosts (not being used right now)
/private/etc/apache2/users/ualter.conf


