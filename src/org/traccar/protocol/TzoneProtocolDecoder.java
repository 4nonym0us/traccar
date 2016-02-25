/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.net.SocketAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class TzoneProtocolDecoder extends BaseProtocolDecoder {

    public TzoneProtocolDecoder(TzoneProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length
        if (buf.readUnsignedShort() != 0x2424) {
            return null;
        }
        buf.readUnsignedShort(); // model
        buf.readUnsignedInt(); // firmware

        String imei = ChannelBuffers.hexDump(buf.readBytes(8)).substring(1);
        if (!identify(imei, channel, remoteAddress)) {
            return null;
        }

        buf.skipBytes(6); // device time

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        // GPS info

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength == 0) {
            return null;
        }

        position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());

        double lat = buf.readUnsignedInt() / 600000.0;
        double lon = buf.readUnsignedInt() / 600000.0;

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
        position.setTime(dateBuilder.getDate());

        position.setSpeed(buf.readUnsignedShort() * 0.01);

        position.set(Event.KEY_ODOMETER, buf.readUnsignedMedium());

        int flags = buf.readUnsignedShort();
        position.setCourse(BitUtil.to(flags, 9));
        if (!BitUtil.check(flags, 10)) {
            lat = -lat;
        }
        position.setLatitude(lat);
        if (BitUtil.check(flags, 9)) {
            lon = -lon;
        }
        position.setLongitude(lon);
        position.setValid(BitUtil.check(flags, 11));

        buf.readerIndex(blockEnd);

        // LBS info

        blockLength = buf.readUnsignedShort();
        blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {

            position.set(Event.KEY_LAC, buf.readUnsignedShort());
            position.set(Event.KEY_CID, buf.readUnsignedShort());

        }

        buf.readerIndex(blockEnd);

        // Status info

        blockLength = buf.readUnsignedShort();
        blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {

            position.set(Event.KEY_ALARM, buf.readUnsignedByte());
            buf.readUnsignedByte(); // terminal info
            position.set(Event.PREFIX_IO + 1, buf.readUnsignedShort());
            position.set(Event.KEY_GSM, buf.readUnsignedByte());
            buf.readUnsignedByte(); // GSM status
            position.set(Event.KEY_BATTERY, buf.readUnsignedShort());
            position.set(Event.KEY_POWER, buf.readUnsignedShort());
            position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Event.PREFIX_ADC + 2, buf.readUnsignedShort());
            position.set(Event.PREFIX_TEMP + 1, buf.readUnsignedShort());

        }

        buf.readerIndex(blockEnd);

        // Cards

        int index = 1;
        for (int i = 0; i < 4; i++) {

            blockLength = buf.readUnsignedShort();
            blockEnd = buf.readerIndex() + blockLength;

            if (blockLength > 0) {

                int count = buf.readUnsignedByte();
                for (int j = 0; j < count; j++) {

                    int length = buf.readUnsignedByte();

                    boolean odd = length % 2 != 0;
                    if (odd) {
                        length += 1;
                    }

                    String num = ChannelBuffers.hexDump(buf.readBytes(length / 2));

                    if (odd) {
                        num = num.substring(1);
                    }

                    position.set("card" + index, num);
                }
            }

            buf.readerIndex(blockEnd);
        }

        buf.skipBytes(buf.readUnsignedShort()); // temperature
        buf.skipBytes(buf.readUnsignedShort()); // lock

        // Passengers

        blockLength = buf.readUnsignedShort();
        blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {

            position.set("passengers-on", buf.readUnsignedMedium());
            position.set("passengers-off", buf.readUnsignedMedium());

        }

        buf.readerIndex(blockEnd);

        return position;
    }

}
