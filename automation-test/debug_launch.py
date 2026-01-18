
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), 'lib'))
from utils import ADBHelper

package = "com.example.launch.aosp.light"
activity = "com.example.launch.aosp.MainActivity"
print(f"Testing start_activity for {package}/{activity}")

success = ADBHelper.start_activity(package, activity)
print(f"Result: {success}")
