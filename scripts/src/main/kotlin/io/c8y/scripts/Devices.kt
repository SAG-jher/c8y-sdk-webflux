package io.c8y.scripts

import com.cumulocity.model.AgentFragment
import io.c8y.api.inventory.*
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicLong

fun main() {
    val management = Platform["local"]
    val rest = management.forTenant( management.rest().tenant().ensureTenant("jaro-0").block())
    val inventoryApi = rest.rest().inventory()

    inventoryApi.listDevices()
        .filter {
            it.get()?.containsKey("schindler_ConnectionStats") == false ||
                    it.get()?.get(AgentFragment)?.equals( mapOf<String,Any>()) != true
        }
        .buffer(200)
        .concatMap { devices ->
            Flux.fromIterable(devices).flatMap { device ->
                inventoryApi.update(
                    device.id!!,
                    AgentFragment to mapOf<String, Any>(),
                    "c8y_Firmware" to mapOf(
                        "name" to "cube-imx8-c8y-buster-arm64-prod-schindler",
                        "version" to "4.0.1_2022-02-15-1215",
                        "url" to null
                    ),
                    "tempPh1Cb4TSE" to "d6LALTE",
                    "c8y_SoftwareList" to listOf(
                        mapOf("name" to "cube-broker", "version" to "0.3.1+deb10", "url" to ""),
                        mapOf("name" to "digital-input-adapter", "version" to "0.4.0+deb10", "url" to ""),
                        mapOf("name" to "enable-native-ssh-prod", "version" to "2.2.4", "url" to ""),
                        mapOf("name" to "etm-comm", "version" to "2.1.1", "url" to ""),
                        mapOf("name" to "ip-forward", "version" to "0.2.5+deb10", "url" to ""),
                        mapOf("name" to "libstateproxy-runtime", "version" to "2.1.1", "url" to ""),
                        mapOf("name" to "python", "version" to "2.7.16-1", "url" to ""),

                        mapOf("name" to "python3", "version" to "3.7.3-1", "url" to ""),
                        mapOf("name" to "python3-pydantic", "version" to "1.7.3-1", "url" to ""),
                        mapOf("name" to "python3-rad", "version" to "1.1.1deb101", "url" to ""),
                        mapOf("name" to "schindler-openvpn", "version" to "1.5.0+deb10", "url" to ""),
                        mapOf("name" to "serial-connector-ec", "version" to "0.4.0+deb10", "url" to ""),
                        mapOf("name" to "third-party-controller-adapter", "version" to "0.4.1+deb10", "url" to ""),
                        mapOf("name" to "tmdevice", "version" to "2.1.1", "url" to ""),
                        mapOf("name" to "btproxy", "version" to "3.2.6+deb10", "url" to ""),
                        mapOf("name" to "cellular-manager-tse", "version" to "1.0.1+deb10", "url" to ""),
                        mapOf("name" to "cube-gateway", "version" to "2.3.1+deb10", "url" to ""),
                        mapOf("name" to "cube-system-adapter", "version" to "5.4.10+deb10", "url" to ""),
                        mapOf("name" to "escalator-modbus-adapter", "version" to "2.0.9+deb10", "url" to ""),
                        mapOf("name" to "etm-adapter", "version" to "4.1.6+deb10", "url" to ""),
                        mapOf("name" to "hardware-broker", "version" to "0.11.0+deb10", "url" to ""),
                        mapOf("name" to "msk-port-forwarding", "version" to "0.2.4+deb10", "url" to ""),
                        mapOf("name" to "python-eebroker", "version" to "1.0.4+deb10", "url" to ""),
                        mapOf("name" to "python-schindler-utils", "version" to "2.3.2+deb10", "url" to ""),
                        mapOf("name" to "python-srmp", "version" to "1.1.1+deb10", "url" to ""),
                        mapOf("name" to "python-telealarm-utils", "version" to "1.5.5+deb10", "url" to ""),
                        mapOf("name" to "python-vcom-protocol", "version" to "2.3.3+deb10", "url" to ""),
                        mapOf("name" to "python3-c8yda", "version" to "0.42.3+deb10-1", "url" to ""),
                        mapOf("name" to "python3-schindler-utils", "version" to "2.3.2+deb10", "url" to ""),
                        mapOf("name" to "remote-monitoring-configurator", "version" to "4.2.5+deb10", "url" to ""),
                        mapOf("name" to "reset-manager", "version" to "0.2.0+deb10", "url" to ""),
                        mapOf("name" to "rmp-gateway", "version" to "3.1.9+deb10", "url" to ""),
                        mapOf("name" to "schindler-eebroker", "version" to "2.0.2+deb10", "url" to ""),
                        mapOf("name" to "tm1-modem-emulator-adapter", "version" to "1.1.8+deb10", "url" to ""),
                        mapOf("name" to "vcom-elevator-adapter", "version" to "3.2.7+deb10", "url" to "")
                    ),
                    "schindler_ConnectionStats" to mapOf(
                        "offline" to 1.93,
                        "lastUpdated" to "2022-04-22T11:11:10Z",
                        "total" to 1303686,
                        "onlineRecent" to 100.0,
                        "onlineRecentPeriod" to 86400.0,
                        "online" to 98.07,
                        "onlineSince" to 130427,
                        "onlineRecentTransitions" to 0
                    ),
                    "flappingUnit" to "true",
                    "c8y_Mobile" to mapOf(
                        "rssi" to -72,
                        "pdpContext" to 3,
                        "currentOperator" to "Telekom.de",
                        "rsrp" to -103,
                        "imsi" to "262017646934084",
                        "ecio" to null,
                        "lastCall" to "2022-04-20T17 to 18:01Z",
                        "sinr" to 13,
                        "cellularTraffic" to 386.58,
                        "iccid" to "89490200001800485354",
                        "currentOperatorChanged" to "2022-04-07T06:22:10Z",
                        "rsrq" to -13,
                        "rscp" to null,
                        "connectedTechnology" to "LTE",
                        "previousOperator" to "-"
                    ),
                    "schindler_RemoteConfiguration" to mapOf(
                        "backend_evolution_stage" to "p",
                        "eenet_ipv6_address" to "fed0:1874:a201::10:0:a",
                        "servitel_country_code" to "DE",
                        "backend_ipv6_address" to "fed0:1874:a201::10:0:49",
                        "command_names" to listOf("config-common"),
                        "backend_country_code" to "DE",
                        "bridge" to listOf<String>(),
                        "backend_tunnel_interface" to "tun0",
                        "servitel_id" to "144187",
                        "mqtt_broker" to true
                    ),
                    "c8y_Configuration_AGENT_CONFIGURATION" to mapOf(
                        "name" to "SAG Only - Agent - Optimization",
                        "time" to "2022-04-09T11:59:58.534Z",
                        "type" to "AGENT_CONFIGURATION",
                        "url" to "https://t1564.dm-zz-p.ioee10-cloud.com/inventory/binaries/21431137628"
                    ),
                    "c8y_SupportedConfigurations" to listOf(
                        "AGENT_CONFIGURATION",
                        "APPLICATION_CONFIGURATION",
                        "FXS_CONFIGURATION",
                        "OPENVPN_CONFIGURATION"
                    ),
                    "schindler_c8yda" to mapOf(
                        "agentMode" to "0",
                        "osVersion" to "10",
                        "agentVersion" to "1.0.9+deb10"
                    ),
                    "kite_Mobile" to mapOf("msisdn" to "n/a"),
                    "schindler_CoreUpdateStatus" to mapOf(
                        "lastUpdated" to "2019-02-14T10 to 12:08Z",
                        "requestFirmwareVersion" to "4.0.1_2022-02-15-1215",
                        "requestFirmwareName" to "cube-imx8-c8y-buster-arm64-prod-schindler",
                        "artifactName" to "4.0.1_2022-02-15-1215_cube-imx8-c8y-buster-arm64-prod-schindler",
                        "status" to "SUCCESSFUL",
                        "lastFailureReason" to null
                    ),
                    "c8y_Profile" to mapOf(
                        "profileName" to "Release_CAB-665-CB4-TIESSE ONLY OS",
                        "profileId" to "20309454804",
                        "profileExecuted" to false
                    ),
                    "schindler_Services" to mapOf(
                        "lastUpdated" to "2022-04-07T09:03:29Z",
                        "services" to mapOf(
                            "freeswitch" to mapOf("active" to "active", "status" to "running"),
                            "rad" to mapOf("active" to "active", "status" to "running"),
                            "sshd_c8y" to mapOf("active" to "active", "status" to "running"),
                            "ssh" to mapOf("active" to "active", "status" to "running"),
                            "c8yda" to mapOf("active" to "active", "status" to "running")
                        )
                    ),
                    "com_cumulocity_model_Agent" to mapOf<String, Any>(),
                    "c8y_Connection" to mapOf("status" to "CONNECTED"),
                    "c8y_RequiredAvailability" to mapOf("responseInterval" to 90),
                    "schindler_CertificateStats" to mapOf("remainingDays" to 820),
                    "schindler_sk" to mapOf("portForwardingServiceStatus" to "0 (NOT_RUNNING)"),
                    "c8y_SupportedOperations" to listOf(
                        "c8y_Command",
                        "c8y_DeviceProfile",
                        "c8y_Firmware",
                        "c8y_LogfileRequest",
                        "c8y_MeasurementRequestOperation",
                        "c8y_RemoteAccessConnect",
                        "c8y_Restart",
                        "c8y_SoftwareUpdate",
                        "schindler_AgentHealthCheck",
                        "schindler_AppointmentRequest",
                        "schindler_BackupConfiguration",
                        "schindler_ResetConfiguration",
                        "schindler_RestoreConfiguration"
                    ),
                    "schindler_rad" to mapOf(
                        "mode" to "Dependent",
                        "lastUpdated" to "2022-04-22T08:03:33Z",
                        "version" to "1.1.1+deb10",
                        "agentStatus" to "HEALTHY"
                    ),
                    "c8y_Hardware" to mapOf(
                        "serialNumber" to "A1XACPG1QB2146000322",
                        "model" to "CUBE_PLUS_GLOBAL_1",
                        "revision" to "FURTHER_PRODUCTION_RELEASE"
                    ),
                    "flapping_units_statistic_overall_status" to "ONLINE",
                    "schindler_Certificate1" to mapOf(
                        "notAfter" to "2024-07-20T22:57:15",
                        "subject" to "C = ZZ, O = Schindler Digital, CN = cb4-a1xacpg1qb2146000322, OU = ZZ-000000000000",
                        "notBefore" to "2022-04-20T22:57:15"
                    ),
                    "c8y_IsDevice" to mapOf<String, Any>(),
                    "c8y_RemoteAccessList" to listOf(
                        mapOf(
                            "serialVersionUID" to 6652959747455810127,
                            "hostname" to "127.0.0.1",
                            "protocol" to "PASSTHROUGH",
                            "port" to 20022,
                            "credentials" to mapOf(
                                "privateKey" to null,
                                "password" to null,
                                "certificate" to null,
                                "publicKey" to null,
                                "hostKey" to null,
                                "type" to "NONE",
                                "username" to null
                            ),
                            "name" to "Passthrough",
                            "id" to "1"
                        ), mapOf(
                            "serialVersionUID" to 6652959747455810127,
                            "hostname" to "127.0.0.1",
                            "protocol" to "SSH",
                            "port" to 22,
                            "credentials" to mapOf(
                                "privateKey" to "mapOf(cipher)8b8c1699b98e0d29374e862bb0087d4a2c7fe3acadf9c19239f7e9a73351f07390f17448e6b48e6cd045d3c3649c5941f3c441a5c076e8ab18382776d5a6c188db639b7368d6d521638306361059cd136b4d07ca750db04952e5ae87b5c15bd52e445299bda5b5a1fa9213d9b6762b7f837b8db633509c0fd1e08174e40bd5fe8c8378d666186f491aa63ee19930b45771d264c8918a03e9b49d85103473f7ac1ac52d23080b3e6fbfa976b7f6d35eadccb6542015f55a44e4f5590da1e1b90700ebbb4e599f2dd6cea1e50ec6bb09ae71f644a27c1c65d66cae59df63e95d7823b694a4bd763aac2b4b166a03c6124c113027ea0de698e4d1ea5e6c45e9c84a53150e3865830321516ce7c63167d723a794da53c06e23f715636d09f71d89451a786a1b2364d59251c936c90e2073754de9a1f38052a1985fcd307603c4cd849052a41970d43a52c623b81b6014d7ee6edcc025e78531e0d7b3931205c2a8d2b85748d14a46ace2c39469db0fe19c6ca661d6bf6e640c2ed2893090f8c430b4fffd48ee119c89eb091d67d8f31387c339a3e22024008d0bf44e106706203e9316af4f48b1e190679894c72ede7489853a5c4ed330fd9b09af2ecf1509255970a6f3d39358e3dc94958eb43d9377062a5d2aae4c3f36e77e7436841b38707039a342e08fd4e5c99618153831db20ba284bf20d22eeebd4a2bf9903d4ab6cf61203ed68d2d5826cb6784305e0aa693ceab5a1d34f807636aaad2fac6617f03286d88db0962a178e90053b17ecda3fbf370031987aa72f829c3439a3ddfe0acc2303887778079f82c2b80deccb9e9cf22cb24b4c64bcc66c3f99a9b57e6939bb555befe148cbd284c66feeddac7062c0db4dd74cb0c263307b79f566114988720753b06bb7abd522f9f05e4febf7f3fe958e316247f392b805dc72330188d4429e0967ad167b53e6edf1081ec18ca38921d16d8d2667b4eb2580bcb5bf1d8132bec3ea4123bd927aaeb9e1f7987c4eb3ce9a501db7a68ffd31eca28988c59c639cc9297959cf44c6c180a2c684ec3c070a3ad6b3cb007e7f4de81ddaa706c1fbb5c8854b237a1b716ebcee0b561a673216b82858536d834bfebbad6943c3dd6da1f5bb95bd6a1228e1fdaf1f7e063e93dc245bde8b4cd849dd0234a7a700b515bf23a7b76bfd34d58ef5f7bffb4fc42e116254f9d99b84c45ccef657489395b287817fa46fbba9c80b962d9bc25a02af81ef48979025ac4929e99a8c23b0613fcae75d3f44c623abc3e3713b55d3513802873d90e5dfe539ef464dac6e6557fd91b01302565015933be7aaa4f63c0722962cf36312cc8ff12a8c9f7a285fa07162ba54bb209523dfb9d9c7f499054528f7381bb7e34a517ee7e73daec807fade13eb5a742f0761e7b314e0c2eed96fd8588a1c67059c19d468e1d09d721ce2553f789d59aa7e06668045fc1eebd4aa13da1b09506768a5e87832a9e222b63bfd0040fb08bbef910b2b3c3d87ea96e13c005228a7507f3eed74b88230adfe8769a43d62d22b1cb990f3dee26156aabf18a96a74d09648ffdf3aeb520234e97b36d170079166003c4fd2d419a562dfc259046b609ca13568fbf77224f5d22f62d2de07a74086a7817bd4dfd96041c474a96a48eff2880ef9ae731310a83fad9ee8ff76d4ca5bbb8e96917e80e15e76a136ea35b86a2a2f1b11c1018d4d26daf0583298f5eb53277d4c725157a8269b18464fa80a06f8abf3421e507ef861ef8c66b5fef45835c78f1889e469ca4d3dde9c763b385db2d699ff94f939501f62ea3bbce9e21a0db987cfc22a2901e37e07c77b34723d3a46d8dc467f15edc97e1bc5d8c9540c8ae4acdfc1d4dd84debf23b501e04d206778819a5c18466b075623cbe7b99f9740b73040824f3719fd107e67285fc90933380928c862a7e83c1a4411ac89ae347896fc2633fa5a3a010b96a832f8a4e762364e570cedd54c2a550650085189e90cb8ce792c126be9550a2485c885efe49f85a2a0e296b3df7b2443e4d2e8a18c6faff43df5f34321db816618471ec03396dc03e082dedfa5103085717c7afa4b11b4f5184871ed8b7e5d3677d3114cf49d3f6dafd8b3b7f88540f0900525776a4a3e05c67e708ffa17d8d18fef49cb6d13ffe50a7c283e741aad2e24120d90b5600807300e1c39b07c606f121dc6770d5e68025997ab9e4d23b6f4ffa69d2220b2580fedf9a0d503e096d49085421522a94ed7e935c1c65bfa9a4c9d3863ac7235a8a94b5fc88086b9b1b55228911eef060d8d68b0a0947cddaf78d43c610b0fe61cba48ef9b15acd2a73b8c822ddf7a83bdf1bbd65f0e5b6273db256f26a87881e5e5aa81df6e383c26eb509327365757acc6f918373ac128863fb56b",
                                "password" to null,
                                "certificate" to null,
                                "publicKey" to "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCwERQxLyiYRYYsfs79QSRrUOh9MTYHvJ5E87QqbHc466d5AS0PvJM342rnmobVesiuHAA8qKYEXImFIkrsdIwaIcQYTtceH0sPs86VuSYYPFIsJ000euw4dK8NmhqasAVwd8J0x/Y343zqf7cvi3e/UC8/+PNrv42G+VWYFXc4esB+seNMANNN8NlcXh5zOfUtOw5rmADmnIpI6gyMFyYxeKuURfn4iedlaCoWKvH98Z/5Ku8OoTuEVd8uM83mlDRDZL8pplNzTG1c4fY5z9Mjo/kuIjnllQWN7iDZsnf+AtlIPLaHExvkdWxOYlsiAOyQYzpaZ2W+DXmOIdR12ixT service_webssh-integration@127.0.0.1",
                                "hostKey" to null,
                                "type" to "KEY_PAIR",
                                "username" to "cb4adm"
                            ),
                            "name" to "Default Remote Access Endpoint",
                            "id" to "2"
                        )
                    ),
                    "c8y_Battery" to mapOf("status" to 5),
                    "schindler_Hardware" to mapOf(
                        "productionDate" to "2021 CW35",
                        "zone" to "EUROPE",
                        "manufacturer" to "TIESSE"
                    ),
                    "c8y_SupportedLogs" to listOf("agent", "deployment", "rad", "systemd"),
                    "schindler_Certificate4" to mapOf(
                        "notAfter" to "2119-09-12T17:55:17",
                        "subject" to "C = CH, L = Ebikon, ST = LU, O = Schindler Digital, CN = Schindler Digital P-RCA T2 RSA",
                        "notBefore" to "2019-09-12T17:55:17"
                    ),
                    "schindler_device" to mapOf(
                        "lastRestartTime" to "2022-04-07 08:18",
                        "activeInterfaces" to mapOf("switch" to 2),
                        "lastBootType" to 1
                    ),
                    "schindler_Certificate2" to mapOf(
                        "notAfter" to "2069-09-12T18:24:03",
                        "subject" to "C = ZZ, L = Ebikon, ST = LU, O = Schindler Digital, CN = Schindler Digital ZZ P-ICA DeviceID T2 RSA",
                        "notBefore" to "2019-09-12T18:24:03"
                    ),
                    "schindler_Certificate3" to mapOf(
                        "notAfter" to "2094-09-12T18:19:21",
                        "subject" to "C = CH, L = Ebikon, ST = LU, O = Schindler Digital, CN = Schindler Digital ZZ P-IMCA T2 RSA",
                        "notBefore" to "2019-09-12T18:19:21"
                    )
                )
            }.last()




        }
        .blockLast()

//    val counter = AtomicLong()
//
//    inventoryApi.listDevices()
//        .skip(110000)
//        .take(50000)
//        .buffer(5000)
//        .map {
//            ManagedObjectRefCollection(references = it.map { mo -> ManagedObjectRef(mo.toReference()) })
//        }
//        .concatMap {
//
//            inventoryApi.ensureGroup("5k devices ${counter.incrementAndGet()}").flatMap { parent ->
//                inventoryApi.addChildAsset(parent.id!!, it)
//            }
//        }
//        .blockLast()
}


