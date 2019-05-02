import uk.gov.hmrc.zap.logger.ZapLogger.log

log.warn(String.format("|%-6s |%-20s|%-10s|" , "SCANNER ID", "      NAME", "   TYPE"))
log.warn(String.format("|%-6s |%-20s|%-10s|" , "1002", "      Information Disclosure - Sensitive Information in HTTP Referrer Header",  "PASSIVE"))


log.warn(
  s"""
     Scanner ID: 10002
     Name : Information Disclosure - Sensitive Information in HTTP Referrer Header
     ScannerType: Passive
   """)