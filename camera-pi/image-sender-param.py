from socket import *
import subprocess
import picamera
import time
import io
import struct
import sys, getopt
import sendingServer

#################### Global Variables #####################

DEBUG = False

ip_address = '192.168.1.143' # ip address for socket
ip_port = 9000 # port number for socket
server_name = 'Camera'


# get the command line arguments to set DEFAULT auto whate balance

# option -w 
#   value i = incandescent
#   value d = shade
#   value s = sunlight
#   value f = fluorescent
AWB_option = ''
AWB_default = ''

#################### Main ############################
#################### Main ############################
#################### Main ############################
#################### Main ############################

# parse the arg list
try:
    opts, args = getopt.getopt(sys.argv[1:], "w:")
except getopt.GetoptError:
    print("Use right options, dummy!")
    sys.exit(2)

for o, a in opts:
    if o == '-w':
        AWB_option = a

#print("balance ", AWB_option)

if AWB_option == 'i':
    AWB_default = 'incandescent'
elif AWB_option == 'd':
    AWB_default = 'shade'
elif AWB_option == 'f':
    AWB_default = 'fluorescent'
elif AWB_option == 's':
    AWB_default = 'sunlight'
else:
    print("Use right options, dummy!")
    sys.exit(2)

print ("Default auto white balance: ", AWB_default)
#sys.exit(0) 

# set up the socket server
## in effect, Pi process becomes a network server
server = sendingServer.Sending_Server(ip_address, ip_port, server_name)

# create a stream for reading image
stream = io.BytesIO()

# set up the camera for correct capture
with picamera.PiCamera() as camera:
    camera.resolution = (2592,1944)
    camera.quality = 100
    camera.format = 'jpeg'
    camera.sharpness = 80
    camera.hflip = True
    camera.vflip = True
    camera.awb_mode = AWB_default
    camera.sharpness = 100 # this seems to help
    camera.contrast = 50 # this seems to help; could be higher
    camera.meter_mode = 'backlit' # this may or may not help
    
#    camera.exposure_compensation = 25	

	# wait for client connection
    server.awaitConnect()

    # loop capturing and sending images
    while True:

		# get the command
        cmd = server.getCommand()
        cmd_lr = cmd.split(":")
        if (DEBUG): print("left ", cmd_lr[0])

        if (cmd_lr[0] == "set_wb"):
            if (DEBUG): print ("Setting white balance ...")
            if (DEBUG): print("right ", cmd_lr[1])
            if (cmd_lr[1] == "n"):
                camera.awb_mode = 'sunlight'
            elif (cmd_lr[1] == "d"):
                camera.awb_mode = 'shade'
            elif (cmd_lr[1] == "f"):
                 camera.awb_mode = 'fluorescent'
            elif (cmd_lr[1] == "i"):
                 camera.awb_mode = 'incandescent'
        elif (cmd_lr[0] == "send_pic"):
            if (DEBUG): print("Capturing...")
            # acquire the image from the camera
            camera.capture(stream, format='jpeg')
            if (DEBUG): print ("image length = ", stream.tell())
            
            ## send the length of the image
            #connection.write(struct.pack('<L', stream.tell()))
            #connection.flush()
            
            #if (DEBUG): print ("think sent length")
            
            ## send the image (from beginning of stream)
            #stream.seek(0)
            #connection.write(stream.read())
            #connection.flush()
            
            # send the stream
            server.sendStream(stream)
            
            if (DEBUG): print ("think sent image")
            
            # clean up for next image
            stream.seek(0)
            stream.truncate() 
            
        else:
            print("Done!")
            break

        # END if then else for command
        
    # END while capturing           

# close client connection, etc.
server.close()   
print ("Finished!")
