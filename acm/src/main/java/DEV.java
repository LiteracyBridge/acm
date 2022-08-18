import org.literacybridge.acm.utils.ExternalCommandRunner.CommandWrapper;
import org.literacybridge.acm.utils.ExternalCommandRunner.LineHandler;
import org.literacybridge.acm.utils.ExternalCommandRunner.LineProcessorResult;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.literacybridge.acm.utils.ExternalCommandRunner.LineProcessorResult.HANDLED;

public class DEV {
    
    private static class LsWrapper extends CommandWrapper {
        private static final Pattern GOT_LINE = Pattern.compile("(.*)");
        
        public static LsWrapper getTotal() {
            LsWrapper lw = new LsWrapper();
            lw.go();
            return lw;
        }

        @Override
        protected File getRunDirectory() {
            return new File(".");
        }

        @Override
        protected String[] getCommand() {
            return new String[]{"zsh", "-c", "pwd"};
        }

        @Override
        protected List<LineHandler> getLineHandlers() {
            return Collections.singletonList(
                new LineHandler(GOT_LINE, this::gotLine)
            );
        }
        
        private LineProcessorResult gotLine(java.io.Writer writer, java.util.regex.Matcher matcher) {
            pwd = matcher.group(1);
            return HANDLED;    
        }
        
        public String pwd;
    }
    
    public static void main(String[] args) throws Exception {
        LsWrapper lw = LsWrapper.getTotal();
        System.out.printf("total: %s\n", lw.pwd);
    }
}
